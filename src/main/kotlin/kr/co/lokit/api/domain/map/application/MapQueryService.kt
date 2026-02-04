package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse.Companion.toAlbumThumbnails
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.map.mapping.toAlbumMapInfoResponse
import kr.co.lokit.api.domain.map.mapping.toClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class MapQueryService(
    private val mapQueryPort: MapQueryPort,
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val mapClientPort: MapClientPort,
    private val transactionTemplate: TransactionTemplate,
    private val mapPhotosCacheService: MapPhotosCacheService,
) : GetMapUseCase, SearchLocationUseCase {
    override fun home(userId: Long, longitude: Double, latitude: Double): HomeResponse {
        val bBox = BBox.fromCenter(GridValues.HOME_ZOOM_LEVEL, longitude, latitude)

        val (locationFuture, albumsFuture) =
            StructuredConcurrency.run { scope ->
                Pair(
                    scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
                    scope.fork {
                        transactionTemplate.execute {
                            albumRepository
                                .findAllByUserId(userId)
                                .sortedByDescending { it.isDefault }
                        }!!
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
        albumId: Long?
    ): MapPhotosResponse {
        val cacheKey = mapPhotosCacheService.buildCacheKey(zoom, bbox, userId, albumId)
        return if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) {
            mapPhotosCacheService.getClusteredPhotos(zoom, bbox, userId, albumId, cacheKey)
        } else {
            mapPhotosCacheService.getIndividualPhotos(bbox, userId, albumId, cacheKey)
        }
    }

    @Transactional(readOnly = true)
    override fun getClusterPhotos(
        clusterId: String,
        userId: Long?,
        page: Int,
        size: Int,
    ): ClusterPhotosPageResponse {
        val gridCell = ClusterId.parse(clusterId)
        val bbox = gridCell.toBBox()

        return mapQueryPort
            .findPhotosInGridCell(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                userId = userId,
                page = page,
                size = size,
            ).toClusterPhotosPageResponse()
    }

    @Transactional(readOnly = true)
    override fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse {
        val bounds = albumBoundsRepository.findByAlbumIdOrNull(albumId)
        return bounds.toAlbumMapInfoResponse(albumId)
    }

    override fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Int,
        bbox: BBox,
        albumId: Long?,
    ): MapMeResponse {
        val homeBBox = BBox.fromCenter(GridValues.HOME_ZOOM_LEVEL, longitude, latitude)

        val (locationFuture, albumsFuture, photosFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
                    scope.fork {
                        transactionTemplate.execute {
                            albumRepository
                                .findAllByUserId(userId)
                                .sortedByDescending { it.isDefault }
                        }!!
                    },
                    scope.fork {
                        getPhotos(zoom, bbox, userId, albumId)
                    },
                )
            }

        val location = locationFuture.get()
        val formattedLocation = LocationInfoResponse(
            address = AddressFormatter.removeProvinceAndCity(
                AddressFormatter.toRoadHeader(location.address, location.roadName)
            ),
            roadName = location.roadName,
            placeName = location.placeName,
            regionName = AddressFormatter.removeProvinceAndCity(location.regionName),
        )

        val photosResponse = photosFuture.get()
        val albums = albumsFuture.get()

        return MapMeResponse(
            location = formattedLocation,
            boundingBox = homeBBox.toResponse(),
            albums = albums.toAlbumThumbnails(),
            clusters = photosResponse.clusters,
            photos = photosResponse.photos,
        )
    }

    override fun getLocationInfo(longitude: Double, latitude: Double): LocationInfoResponse {
        val raw = mapClientPort.reverseGeocode(longitude, latitude)
        val header = AddressFormatter.toRoadHeader(raw.address, raw.roadName)
        val formattedAddress = AddressFormatter.removeProvinceAndCity(header)
        return raw.copy(address = formattedAddress)
    }

    override fun searchPlaces(query: String): PlaceSearchResponse =
        PlaceSearchResponse(places = mapClientPort.searchPlaces(query))
}
