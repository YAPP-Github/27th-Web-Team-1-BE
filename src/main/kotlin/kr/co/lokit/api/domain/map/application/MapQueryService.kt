package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.concurrency.withPermit
import kr.co.lokit.api.common.dto.isValidId
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse.Companion.toAlbumThumbnails
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.map.mapping.toAlbumMapInfoResponse
import kr.co.lokit.api.domain.map.mapping.toClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Semaphore

@Service
class MapQueryService(
    private val mapQueryPort: MapQueryPort,
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val coupleRepository: CoupleRepositoryPort,
    private val mapClientPort: MapClientPort,
    private val mapPhotosCacheService: MapPhotosCacheService,
) : GetMapUseCase,
    SearchLocationUseCase {
    private val dbSemaphore = Semaphore(6)

    override fun home(
        userId: Long,
        longitude: Double,
        latitude: Double,
    ): HomeResponse {
        val bBox = BBox.fromCenter(GridValues.HOME_ZOOM_LEVEL, longitude, latitude)
        val coupleId = coupleRepository.findByUserId(userId)?.id

        val (locationFuture, albumsFuture) =
            StructuredConcurrency.run { scope ->
                Pair(
                    scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
                    scope.fork {
                        dbSemaphore.withPermit {
                            if (coupleId != null) {
                                albumRepository
                                    .findAllByCoupleId(coupleId)
                                    .sortedByDescending { it.isDefault }
                            } else {
                                emptyList()
                            }
                        }
                    },
                )
            }

        return HomeResponse.of(
            locationFuture.get(),
            albumsFuture.get(),
            bBox,
        )
    }

    @Transactional(readOnly = true)
    override fun getPhotos(
        zoom: Int,
        bbox: BBox,
        userId: Long?,
        albumId: Long?,
    ): MapPhotosResponse {
        val coupleId = userId?.let { coupleRepository.findByUserId(it)?.id }

        val effectiveAlbumId =
            if (isValidId(albumId) && isValidId(userId)) {
                val album = albumRepository.findById(albumId!!)
                if (album?.isDefault == true) null else albumId
            } else {
                albumId
            }

        return if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) {
            mapPhotosCacheService.getClusteredPhotos(zoom, bbox, coupleId, effectiveAlbumId)
        } else {
            mapPhotosCacheService.getIndividualPhotos(
                zoom = zoom,
                bbox = bbox,
                coupleId = coupleId,
                albumId = effectiveAlbumId,
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getClusterPhotos(
        clusterId: String,
        userId: Long?,
    ): List<ClusterPhotoResponse> {
        val coupleId = userId?.let { coupleRepository.findByUserId(it)?.id }
        val gridCell = ClusterId.parse(clusterId)
        val bbox = gridCell.toBBox()

        return mapQueryPort
            .findPhotosInGridCell(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                coupleId = coupleId,
            ).toClusterPhotosPageResponse()
    }

    @Transactional(readOnly = true)
    override fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse {
        val album = albumRepository.findById(albumId)
        val (standardId, idType) = if (album?.isDefault == true) {
            album.coupleId to BoundsIdType.COUPLE
        } else {
            albumId to BoundsIdType.ALBUM
        }
        val bounds = albumBoundsRepository.findByStandardIdAndIdType(standardId, idType)
        return bounds.toAlbumMapInfoResponse(albumId)
    }

    override fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Int,
        bbox: BBox,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeResponse {
        val homeBBox = BBox.fromCenter(zoom, longitude, latitude)
        val coupleId = coupleRepository.findByUserId(userId)?.id
        val currentVersion = mapPhotosCacheService.getVersion(coupleId)
        val versionUnchanged = lastDataVersion != null && lastDataVersion == currentVersion

        val (locationFuture, albumsFuture, photosFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
                    scope.fork {
                        dbSemaphore.withPermit {
                            if (coupleId != null) {
                                albumRepository
                                    .findAllByCoupleId(coupleId)
                                    .sortedByDescending { it.isDefault }
                            } else {
                                emptyList()
                            }
                        }
                    },
                    scope.fork {
                        if (versionUnchanged) {
                            null
                        } else {
                            dbSemaphore.withPermit {
                                getPhotos(zoom, homeBBox, userId, albumId)
                            }
                        }
                    },
                )
            }

        val location = locationFuture.get()
        val formattedLocation =
            LocationInfoResponse(
                address =
                    AddressFormatter.removeProvinceAndCity(
                        AddressFormatter.toRoadHeader(location.address ?: "", location.roadName ?: ""),
                    ),
                roadName = location.roadName,
                placeName = location.placeName,
                regionName = AddressFormatter.removeProvinceAndCity(location.regionName ?: ""),
            )

        val photosResponse = photosFuture.get()
        val albums = albumsFuture.get()

        return MapMeResponse(
            location = formattedLocation,
            boundingBox = homeBBox.toResponse(),
            albums = albums.toAlbumThumbnails(),
            dataVersion = currentVersion,
            clusters = photosResponse?.clusters,
            photos = photosResponse?.photos,
            totalHistoryCount = albumRepository.photoCountSumByUserId(userId),
        )
    }

    override fun getLocationInfo(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse {
        val raw = mapClientPort.reverseGeocode(longitude, latitude)
        val header = AddressFormatter.toRoadHeader(raw.address ?: "", raw.roadName ?: "")
        val formattedAddress = AddressFormatter.removeProvinceAndCity(header)
        return raw.copy(address = formattedAddress)
    }

    override fun searchPlaces(query: String): PlaceSearchResponse =
        PlaceSearchResponse(places = mapClientPort.searchPlaces(query))
}
