package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.concurrency.withPermit
import kr.co.lokit.api.common.dto.isValidId
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
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
import kotlin.math.floor

@Service
class MapQueryService(
    private val mapQueryPort: MapQueryPort,
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val coupleRepository: CoupleRepositoryPort,
    private val mapClientPort: MapClientPort,
    private val mapPhotosCacheService: MapPhotosCacheService,
    private val clusterBoundaryMergeStrategy: ClusterBoundaryMergeStrategy,
) : GetMapUseCase,
    SearchLocationUseCase {
    private val dbSemaphore = Semaphore(6)

    private fun getPhotos(
        zoom: Int,
        bbox: BBox,
        userId: Long?,
        albumId: Long?,
    ): MapPhotosResponse {
        val boundedBbox = bbox.clampToKorea() ?: return emptyPhotosResponse(zoom)
        val coupleId = userId?.let { coupleRepository.findByUserId(it)?.id }
        if (userId != null && coupleId == null) {
            return emptyPhotosResponse(zoom)
        }

        val effectiveAlbumId = resolveEffectiveAlbumId(userId, albumId)

        return if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) {
            mapPhotosCacheService.getClusteredPhotos(zoom, boundedBbox, coupleId, effectiveAlbumId)
        } else {
            mapPhotosCacheService.getIndividualPhotos(
                zoom = zoom,
                bbox = boundedBbox,
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
        if (userId != null && coupleId == null) {
            return emptyList()
        }
        val gridCell = ClusterId.parse(clusterId)
        val expandedBBox = expandedClusterSearchBBox(gridCell) ?: return emptyList()
        val photos =
            mapQueryPort.findPhotosInGridCell(
                west = expandedBBox.west,
                south = expandedBBox.south,
                east = expandedBBox.east,
                north = expandedBBox.north,
                coupleId = coupleId,
            )
        if (photos.isEmpty()) {
            return emptyList()
        }

        val gridSize = GridValues.getGridSize(gridCell.zoom)
        val photosByCell =
            photos.groupBy {
                CellCoord(
                    x = floor(lonToMeters(it.longitude) / gridSize).toLong(),
                    y = floor(latToMeters(it.latitude) / gridSize).toLong(),
                )
            }

        val mergedCells =
            clusterBoundaryMergeStrategy.resolveClusterCells(
                zoom = gridCell.zoom,
                photosByCell = photosByCell.mapValues { (_, v) -> v.map { GeoPoint(it.longitude, it.latitude) } },
                targetCell = CellCoord(gridCell.cellX, gridCell.cellY),
            )
        val target = CellCoord(gridCell.cellX, gridCell.cellY)
        val memberCells = if (mergedCells.isEmpty()) setOf(target) else mergedCells
        return photos
            .filter {
                val cell = CellCoord(floor(lonToMeters(it.longitude) / gridSize).toLong(), floor(latToMeters(it.latitude) / gridSize).toLong())
                cell in memberCells
            }.toClusterPhotosPageResponse()
    }

    @Transactional(readOnly = true)
    override fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse {
        val album = albumRepository.findById(albumId)
        val (standardId, idType) =
            if (album?.isDefault == true) {
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
        @Suppress("UNUSED_PARAMETER") lastDataVersion: Long?,
    ): MapMeResponse {
        val homeBBox = BBox.fromCenter(zoom, longitude, latitude).clampToKorea() ?: BBox.KOREA_BOUNDS
        val coupleId = coupleRepository.findByUserId(userId)?.id
        val effectiveAlbumId = resolveEffectiveAlbumId(userId, albumId)
        val currentVersion = mapPhotosCacheService.getDataVersion(zoom, homeBBox, coupleId, effectiveAlbumId)

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
                        dbSemaphore.withPermit {
                            getPhotos(zoom, homeBBox, userId, effectiveAlbumId)
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

    private fun resolveEffectiveAlbumId(
        userId: Long?,
        albumId: Long?,
    ): Long? =
        if (isValidId(albumId) && isValidId(userId)) {
            val album = albumRepository.findById(albumId!!)
            if (album?.isDefault == true) null else albumId
        } else {
            albumId
        }

    private fun expandedClusterSearchBBox(cell: kr.co.lokit.api.domain.map.domain.GridCell): BBox? {
        val sw = kr.co.lokit.api.domain.map.domain.GridCell(cell.zoom, cell.cellX - 1, cell.cellY - 1).toBBox()
        val ne = kr.co.lokit.api.domain.map.domain.GridCell(cell.zoom, cell.cellX + 1, cell.cellY + 1).toBBox()
        return BBox(
            west = sw.west,
            south = sw.south,
            east = ne.east,
            north = ne.north,
        ).clampToKorea()
    }

    private fun lonToMeters(lon: Double): Double = lon * (Math.PI * 6378137.0 / 180.0)

    private fun latToMeters(lat: Double): Double = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) * 6378137.0

    private fun emptyPhotosResponse(zoom: Int): MapPhotosResponse =
        MapPhotosResponse(
            clusters = if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) emptyList() else null,
            photos = if (zoom >= GridValues.CLUSTER_ZOOM_THRESHOLD) emptyList() else null,
        )
}
