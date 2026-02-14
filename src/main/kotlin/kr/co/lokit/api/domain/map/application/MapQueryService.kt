package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.concurrency.withPermit
import kr.co.lokit.api.common.dto.isValidId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.errorDetailsOf
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
import kr.co.lokit.api.domain.map.domain.MapZoom
import kr.co.lokit.api.domain.map.domain.MercatorProjection
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
    private data class MapViewerContext(
        val userId: Long?,
        val coupleId: Long?,
        val albumId: Long?,
    )

    private val dbSemaphore = Semaphore(6)

    private fun getPhotos(
        zoom: Int,
        zoomLevel: Double,
        bbox: BBox,
        context: MapViewerContext,
        lastDataVersion: Long?,
        currentDataVersion: Long,
    ): MapPhotosResponse {
        val boundedBbox = bbox.clampToKorea() ?: return emptyPhotosResponse(zoom)
        if (isMissingCoupleForAuthenticatedUser(context.userId, context.coupleId)) {
            return emptyPhotosResponse(zoom)
        }
        val canReuseCellCache = lastDataVersion != null && lastDataVersion == currentDataVersion

        return if (zoomLevel < GridValues.CLUSTER_ZOOM_THRESHOLD.toDouble()) {
            mapPhotosCacheService.getClusteredPhotos(
                zoom = zoom,
                zoomLevel = zoomLevel,
                bbox = boundedBbox,
                coupleId = context.coupleId,
                albumId = context.albumId,
                canReuseCellCache = canReuseCellCache,
            )
        } else {
            mapPhotosCacheService.getIndividualPhotos(
                zoom = zoom,
                bbox = boundedBbox,
                coupleId = context.coupleId,
                albumId = context.albumId,
            )
        }
    }

    @Transactional(readOnly = true)
    override fun getClusterPhotos(
        clusterId: String,
        userId: Long?,
    ): List<ClusterPhotoResponse> {
        val context = resolveViewerContext(userId = userId, albumId = null)
        if (isMissingCoupleForAuthenticatedUser(context.userId, context.coupleId)) {
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
                coupleId = context.coupleId,
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
                val cell =
                    CellCoord(
                        floor(lonToMeters(it.longitude) / gridSize).toLong(),
                        floor(latToMeters(it.latitude) / gridSize).toLong(),
                    )
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
        zoom: Double,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeResponse =
        getMeByBBox(
            userId = userId,
            centerLongitude = longitude,
            centerLatitude = latitude,
            zoom = zoom,
            bbox =
                BBox
                    .fromCenter(MapZoom.from(zoom).discrete, longitude, latitude)
                    .clampToKorea() ?: BBox.KOREA_BOUNDS,
            albumId = albumId,
            lastDataVersion = lastDataVersion,
        )

    override fun getMe(
        userId: Long,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        zoom: Double,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeResponse {
        val requestedBBox =
            try {
                BBox.fromRequestedBoundsInKorea(
                    west = west,
                    south = south,
                    east = east,
                    north = north,
                )
            } catch (e: IllegalArgumentException) {
                throw BusinessException.InvalidInputException(
                    message = "유효하지 않은 BBox 범위입니다",
                    cause = e,
                    errors = errorDetailsOf("west" to west, "south" to south, "east" to east, "north" to north),
                )
            }
        val centerLongitude = (requestedBBox.west + requestedBBox.east) / 2.0
        val centerLatitude = (requestedBBox.south + requestedBBox.north) / 2.0

        return getMeByBBox(
            userId = userId,
            centerLongitude = centerLongitude,
            centerLatitude = centerLatitude,
            zoom = zoom,
            bbox = requestedBBox,
            albumId = albumId,
            lastDataVersion = lastDataVersion,
        )
    }

    override fun getLocationInfo(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse {
        val raw = mapClientPort.reverseGeocode(longitude, latitude)
        val header = AddressFormatter.toRoadHeader(raw.address.orEmpty(), raw.roadName.orEmpty())
        val formattedAddress = AddressFormatter.removeProvinceAndCity(header)
        return raw.copy(address = formattedAddress)
    }

    override fun searchPlaces(query: String): PlaceSearchResponse =
        PlaceSearchResponse(places = mapClientPort.searchPlaces(query))

    private fun getMeByBBox(
        userId: Long,
        centerLongitude: Double,
        centerLatitude: Double,
        zoom: Double,
        bbox: BBox,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeResponse {
        val mapZoom = MapZoom.from(zoom)
        val context = resolveViewerContext(userId = userId, albumId = albumId)
        val currentVersion =
            mapPhotosCacheService.getDataVersion(
                _zoom = mapZoom.discrete,
                _bbox = bbox,
                coupleId = context.coupleId,
                albumId = context.albumId,
            )

        val (locationFuture, albumsFuture, photosFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { mapClientPort.reverseGeocode(centerLongitude, centerLatitude) },
                    scope.fork {
                        dbSemaphore.withPermit {
                            findAlbumsForCouple(context.coupleId)
                        }
                    },
                    scope.fork {
                        dbSemaphore.withPermit {
                            getPhotos(
                                zoom = mapZoom.discrete,
                                zoomLevel = mapZoom.level,
                                bbox = bbox,
                                context = context,
                                lastDataVersion = lastDataVersion,
                                currentDataVersion = currentVersion,
                            )
                        }
                    },
                )
            }

        val formattedLocation = formatLocation(locationFuture.get())
        val photosResponse = photosFuture.get()
        val albums = albumsFuture.get()

        return MapMeResponse(
            location = formattedLocation,
            boundingBox = bbox.toResponse(),
            albums = albums.toAlbumThumbnails(),
            dataVersion = currentVersion,
            clusters = photosResponse?.clusters,
            photos = photosResponse?.photos,
            totalHistoryCount = albumRepository.photoCountSumByUserId(userId),
        )
    }

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

    private fun resolveOptionalCoupleId(userId: Long?): Long? = userId?.let { coupleRepository.findByUserId(it)?.id }

    private fun resolveViewerContext(
        userId: Long?,
        albumId: Long?,
    ): MapViewerContext {
        val coupleId = resolveOptionalCoupleId(userId)
        val effectiveAlbumId = resolveEffectiveAlbumId(userId, albumId)
        return MapViewerContext(userId = userId, coupleId = coupleId, albumId = effectiveAlbumId)
    }

    private fun findAlbumsForCouple(coupleId: Long?) =
        coupleId
            ?.let { albumRepository.findAllByCoupleId(it).sortedByDescending { album -> album.isDefault } }.orEmpty()

    private fun formatLocation(location: LocationInfoResponse): LocationInfoResponse =
        LocationInfoResponse(
            address =
                AddressFormatter.removeProvinceAndCity(
                    AddressFormatter.toRoadHeader(location.address.orEmpty(), location.roadName.orEmpty()),
                ),
            roadName = location.roadName,
            placeName = location.placeName,
            regionName = AddressFormatter.removeProvinceAndCity(location.regionName.orEmpty()),
        )

    private fun isMissingCoupleForAuthenticatedUser(
        userId: Long?,
        coupleId: Long?,
    ): Boolean = userId != null && coupleId == null

    private fun expandedClusterSearchBBox(cell: kr.co.lokit.api.domain.map.domain.GridCell): BBox? {
        val sw =
            kr.co.lokit.api.domain.map.domain
                .GridCell(cell.zoom, cell.cellX - 1, cell.cellY - 1)
                .toBBox()
        val ne =
            kr.co.lokit.api.domain.map.domain
                .GridCell(cell.zoom, cell.cellX + 1, cell.cellY + 1)
                .toBBox()
        return BBox(
            west = sw.west,
            south = sw.south,
            east = ne.east,
            north = ne.north,
        ).clampToKorea()
    }

    private fun lonToMeters(lon: Double): Double = MercatorProjection.longitudeToMeters(lon)

    private fun latToMeters(lat: Double): Double = MercatorProjection.latitudeToMeters(lat)

    private fun emptyPhotosResponse(zoom: Int): MapPhotosResponse =
        MapPhotosResponse(
            clusters = if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) emptyList() else null,
            photos = if (zoom >= GridValues.CLUSTER_ZOOM_THRESHOLD) emptyList() else null,
        )
}
