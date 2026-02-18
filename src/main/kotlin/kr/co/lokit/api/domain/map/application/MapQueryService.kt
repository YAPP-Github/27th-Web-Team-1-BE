package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.concurrency.withPermit
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
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
import kr.co.lokit.api.domain.map.domain.AlbumThumbnailsReadModel
import kr.co.lokit.api.domain.map.domain.AlbumThumbnails
import kr.co.lokit.api.domain.map.domain.MapZoom
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import kr.co.lokit.api.domain.map.domain.AlbumMapInfoReadModel
import kr.co.lokit.api.domain.map.domain.ClusterPhotoReadModel
import kr.co.lokit.api.domain.map.domain.ClusterPhotos
import kr.co.lokit.api.domain.map.domain.Clusters
import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.MapMeReadModel
import kr.co.lokit.api.domain.map.domain.MapPhotosReadModel
import kr.co.lokit.api.domain.map.domain.MapPhotos
import kr.co.lokit.api.domain.map.domain.PlaceSearchReadModel
import kr.co.lokit.api.domain.map.domain.Places
import kr.co.lokit.api.domain.map.domain.ThumbnailUrls
import kr.co.lokit.api.domain.map.application.mapping.toAlbumMapReadModel
import kr.co.lokit.api.domain.map.application.mapping.toClusterPhotoReadModels
import kr.co.lokit.api.domain.map.application.mapping.toBoundingBoxReadModel
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
        zoom: Double,
        bbox: BBox,
        context: MapViewerContext,
        lastDataVersion: Long?,
        currentDataVersion: Long,
    ): MapPhotosReadModel {
        val boundedBbox = bbox.clampToKorea() ?: return emptyPhotosResponse(zoom)
        if (isMissingCoupleForAuthenticatedUser(context.userId, context.coupleId)) {
            return emptyPhotosResponse(zoom)
        }
        val canReuseCellCache = lastDataVersion != null && lastDataVersion == currentDataVersion

        return if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD.toDouble()) {
            mapPhotosCacheService.getClusteredPhotos(
                zoom = zoom,
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
    ): ClusterPhotos {
        val context = resolveViewerContext(userId = userId, albumId = null)
        if (isMissingCoupleForAuthenticatedUser(context.userId, context.coupleId)) {
            return ClusterPhotos.empty()
        }
        val parsedClusterId = ClusterId.parseDetailed(clusterId)
        val gridCell = kr.co.lokit.api.domain.map.domain.GridCell(parsedClusterId.zoom, parsedClusterId.cellX, parsedClusterId.cellY)
        val expandedBBox = expandedClusterSearchBBox(gridCell) ?: return ClusterPhotos.empty()
        val photos =
            mapQueryPort.findPhotosInGridCell(
                west = expandedBBox.west,
                south = expandedBBox.south,
                east = expandedBBox.east,
                north = expandedBBox.north,
                coupleId = context.coupleId,
            )
        if (photos.isEmpty()) {
            return ClusterPhotos.empty()
        }

        val gridSize = GridValues.getGridSize(gridCell.zoom)
        val members =
            photos.map { photo ->
                val cell =
                    CellCoord(
                        x = floor(lonToMeters(photo.longitude) / gridSize).toLong(),
                        y = floor(latToMeters(photo.latitude) / gridSize).toLong(),
                    )
                ClusterPhotoMember(
                    id = photo.id,
                    cell = cell,
                    point = GeoPoint(photo.longitude, photo.latitude),
                )
            }
        val memberPhotoIds =
            clusterBoundaryMergeStrategy.resolveClusterPhotoIds(
                zoom = parsedClusterId.mergeZoom ?: parsedClusterId.zoom.toDouble(),
                photos = members,
                targetClusterId = clusterId,
            )
        if (memberPhotoIds.isEmpty()) {
            return ClusterPhotos.empty()
        }
        return ClusterPhotos.of(
            photos
                .filter { it.id in memberPhotoIds }
                .toClusterPhotoReadModels(),
        )
    }

    @Transactional(readOnly = true)
    override fun getAlbumMapInfo(albumId: Long): AlbumMapInfoReadModel {
        val album = albumRepository.findById(albumId)
        val (standardId, idType) =
            if (album?.isDefault == true) {
                album.coupleId to BoundsIdType.COUPLE
            } else {
                albumId to BoundsIdType.ALBUM
            }
        val bounds = albumBoundsRepository.findByStandardIdAndIdType(standardId, idType)
        return bounds.toAlbumMapReadModel(albumId)
    }

    override fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Double,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeReadModel {
        val bbox =
            BBox
                .fromCenter(MapZoom.from(zoom).level, longitude, latitude)
                .clampToKorea() ?: BBox.KOREA_BOUNDS

        val mapZoom = MapZoom.from(zoom)
        val context = resolveViewerContext(userId = userId, albumId = albumId)
        val currentVersion =
            mapPhotosCacheService.getDataVersion(
                zoom = mapZoom.level,
                bbox = bbox,
                coupleId = context.coupleId,
                albumId = context.albumId,
            )

        val (locationFuture, albumsFuture, photosFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
                    scope.fork {
                        dbSemaphore.withPermit {
                            findAlbumsForCouple(context.coupleId)
                        }
                    },
                    scope.fork {
                        dbSemaphore.withPermit {
                            getPhotos(
                                zoom = mapZoom.level,
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

        return MapMeReadModel(
            location = formattedLocation,
            boundingBox = bbox.toBoundingBoxReadModel(),
            albums = albums.toAlbumThumbnailsReadModels(),
            dataVersion = currentVersion,
            clusters = photosResponse.clusters,
            photos = photosResponse.photos,
            totalHistoryCount = albumRepository.photoCountSumByUserId(userId),
        )
    }

    override fun getLocationInfo(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel {
        val raw = mapClientPort.reverseGeocode(longitude, latitude)
        val header = AddressFormatter.toRoadHeader(raw.address.orEmpty(), raw.roadName.orEmpty())
        val formattedAddress = AddressFormatter.removeProvinceAndCity(header)
        return raw.copy(address = formattedAddress)
    }

    override fun searchPlaces(query: String): PlaceSearchReadModel =
        PlaceSearchReadModel(places = mapClientPort.searchPlaces(query))

    private fun getMeByBBox(
        userId: Long,
        centerLongitude: Double,
        centerLatitude: Double,
        zoom: Double,
        bbox: BBox,
        albumId: Long?,
        lastDataVersion: Long?,
    ): MapMeReadModel {
        val mapZoom = MapZoom.from(zoom)
        val context = resolveViewerContext(userId = userId, albumId = albumId)
        val currentVersion =
            mapPhotosCacheService.getDataVersion(
                zoom = mapZoom.level,
                bbox = bbox,
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
                                zoom = mapZoom.level,
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

        return MapMeReadModel(
            location = formattedLocation,
            boundingBox = bbox.toBoundingBoxReadModel(),
            albums = albums.toAlbumThumbnailsReadModels(),
            dataVersion = currentVersion,
            clusters = photosResponse.clusters,
            photos = photosResponse.photos,
            totalHistoryCount = albumRepository.photoCountSumByUserId(userId),
        )
    }

    private fun resolveEffectiveAlbumId(
        userId: Long?,
        albumId: Long?,
    ): Long? =
        if (albumId.isPositiveId() && userId.isPositiveId()) {
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
            ?.let { albumRepository.findAllByCoupleId(it).sortedByDescending { album -> album.isDefault } }
            .orEmpty()

    private fun formatLocation(location: LocationInfoReadModel): LocationInfoReadModel =
        LocationInfoReadModel(
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

    private fun emptyPhotosResponse(zoom: Double): MapPhotosReadModel =
        MapPhotosReadModel(
            clusters = if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD.toDouble()) Clusters.empty() else null,
            photos = if (zoom >= GridValues.CLUSTER_ZOOM_THRESHOLD.toDouble()) MapPhotos.empty() else null,
        )

    private fun List<Album>.toAlbumThumbnailsReadModels(): AlbumThumbnails =
        AlbumThumbnails.of(
            map { album ->
                val actualPhotoCount = if (album.isDefault) album.photos.size else album.photoCount
                AlbumThumbnailsReadModel(
                    id = album.id,
                    title = album.title,
                    photoCount = actualPhotoCount,
                    thumbnailUrls = ThumbnailUrls.of(album.thumbnails.map { it.url }),
                )
            },
        )

    private fun Long?.isPositiveId(): Boolean = this != null && this > 0L
}
