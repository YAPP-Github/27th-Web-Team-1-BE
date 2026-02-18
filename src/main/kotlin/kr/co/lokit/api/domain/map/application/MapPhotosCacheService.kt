package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.util.orZero
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.config.cache.CachePolicy
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.MapGridIndex
import kr.co.lokit.api.domain.map.domain.MapPhotos
import kr.co.lokit.api.domain.map.domain.MapMutationTracker
import kr.co.lokit.api.domain.map.domain.MapViewportTracker
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import kr.co.lokit.api.domain.map.domain.Clusters
import kr.co.lokit.api.domain.map.domain.MapPhotosReadModel
import kr.co.lokit.api.domain.map.application.mapping.toMapPhotoReadModel
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Semaphore
import kotlin.math.floor

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
    private val cacheManager: CacheManager,
    private val clusterBoundaryMergeStrategy: ClusterBoundaryMergeStrategy,
) {
    data class CachedCell(
        val response: ClusterReadModel?,
    )

    private val prefetchSemaphore = Semaphore(CachePolicy.MAP_PREFETCH_CONCURRENCY)
    private val mutationTracker = MapMutationTracker(CachePolicy.MAP_MAX_MUTATIONS_PER_COUPLE)
    private val viewportTracker = MapViewportTracker()

    fun evictForCouple(coupleId: Long) {
        mutationTracker.evictForCouple(coupleId)
        evictCoupleEntries(CacheNames.MAP_PHOTOS, coupleId)
        viewportTracker.clearForCouple(coupleId)
    }

    fun getVersion(coupleId: Long?): Long = mutationTracker.currentSequence(coupleId)

    fun getVersion(
        zoom: Double,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Long = mutationTracker.viewportSequence(bbox = bbox, coupleId = coupleId, albumId = albumId)

    fun getDataVersion(
        zoom: Double,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Long = mutationTracker.dataVersion(coupleId = coupleId, albumId = albumId)

    fun evictForPhotoMutation(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ) {
        mutationTracker.recordMutation(coupleId, albumId, longitude, latitude)
        evictIndividualEntriesForPoint(coupleId, albumId, longitude, latitude)
        viewportTracker.clearForCouple(coupleId)
    }

    @Suppress("UNUSED_PARAMETER")
    fun getClusteredPhotos(
        zoom: Double,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        canReuseCellCache: Boolean = true,
    ): MapPhotosReadModel = queryRawPoiClusters(zoom, bbox, coupleId, albumId)

    private fun queryRawPoiClusters(
        zoom: Double,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): MapPhotosReadModel {
        val discreteZoom = floor(zoom).toInt()
        val clusterGridSize = GridValues.getGridSize(discreteZoom)
        val prefetchGridSize = clusterGridSize
        val sequence = mutationTracker.currentSequence(coupleId)
        val requestedWindow = MapGridIndex.toCellWindow(bbox, prefetchGridSize)

        val clusters =
            mapQueryPort.findPhotosWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                coupleId = coupleId,
                albumId = albumId,
            ).map { photo ->
                    val (cellX, cellY) = MapGridIndex.toCell(photo.longitude, photo.latitude, clusterGridSize)
                    ClusterReadModel(
                        clusterId = ClusterId.format(discreteZoom, cellX, cellY),
                        count = 1,
                        thumbnailUrl = photo.url,
                        longitude = photo.longitude,
                        latitude = photo.latitude,
                        takenAt = photo.takenAt,
                    )
                }

        val prefetchCoords =
            viewportTracker.directionalPrefetchCells(
                zoom = discreteZoom,
                gridSize = prefetchGridSize,
                requestedWindow = requestedWindow,
                bbox = bbox,
                coupleId = coupleId,
                albumId = albumId,
            )

        scheduleRawPrefetch(
            gridSize = prefetchGridSize,
            coupleId = coupleId,
            albumId = albumId,
            sequence = sequence,
            coords = prefetchCoords,
        )

        return MapPhotosReadModel(clusters = Clusters.of(clusterBoundaryMergeStrategy.mergeClusters(clusters, discreteZoom)))
    }

    private fun scheduleRawPrefetch(
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
        sequence: Long,
        coords: Set<Pair<Long, Long>>,
    ) {
        if (coords.isEmpty()) {
            return
        }
        if (!prefetchSemaphore.tryAcquire()) {
            return
        }

        Thread.startVirtualThread {
            try {
                if (coupleId != null && mutationTracker.currentSequence(coupleId) != sequence) {
                    return@startVirtualThread
                }
                val bounds = MapGridIndex.toMeterBounds(coords, gridSize)

                mapQueryPort.findPhotosWithinBBox(
                    west = MercatorProjection.metersToLongitude(bounds.west),
                    south = MercatorProjection.metersToLatitude(bounds.south),
                    east = MercatorProjection.metersToLongitude(bounds.east),
                    north = MercatorProjection.metersToLatitude(bounds.north),
                    coupleId = coupleId,
                    albumId = albumId,
                )
            } finally {
                prefetchSemaphore.release()
            }
        }
    }

    private fun evictCoupleEntries(
        cacheName: String,
        coupleId: Long,
    ) {
        val cache = cacheManager.getCache(cacheName) as? CaffeineCache ?: return
        val marker = "_c${coupleId}_"
        val keysToInvalidate =
            cache.nativeCache
                .asMap()
                .keys
                .asSequence()
                .filterIsInstance<String>()
                .filter { it.contains(marker) }
                .toList()
        if (keysToInvalidate.isNotEmpty()) {
            cache.nativeCache.invalidateAll(keysToInvalidate)
        }
    }

    private fun evictIndividualEntriesForPoint(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ) {
        val cache = cacheManager.getCache(CacheNames.MAP_PHOTOS) as? CaffeineCache ?: return
        val keys =
            cache.nativeCache
                .asMap()
                .keys
                .asSequence()
                .filterIsInstance<String>()
                .toList()
        if (keys.isEmpty()) {
            return
        }

        val targetAlbum = albumId.orZero()
        val keysToInvalidate =
            keys.filter { key ->
                val parsed = MapCacheKeyFactory.parseIndividualKey(key) ?: return@filter false
                if (parsed.coupleId != coupleId) return@filter false
                if (parsed.albumId != 0L && parsed.albumId != targetAlbum) return@filter false
                longitude in parsed.west..parsed.east && latitude in parsed.south..parsed.north
            }
        if (keysToInvalidate.isNotEmpty()) {
            cache.nativeCache.invalidateAll(keysToInvalidate)
        }
    }

    fun buildCellKey(
        zoom: Int,
        cellX: Long,
        cellY: Long,
        coupleId: Long?,
        albumId: Long?,
        version: Long,
    ): String = MapCacheKeyFactory.buildCellKey(zoom, cellX, cellY, coupleId, albumId, version)

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = [CacheNames.MAP_PHOTOS],
        key =
            "T(kr.co.lokit.api.domain.map.application.MapCacheKeyFactory)" +
                ".buildIndividualKey(#bbox, #zoom, #coupleId, #albumId, @mapPhotosCacheService.getVersion(#zoom, #bbox, #coupleId, #albumId))",
        unless = "#result.photos == null || #result.photos.isEmpty()",
    )
    fun getIndividualPhotos(
        zoom: Double,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): MapPhotosReadModel {
        val photos =
            mapQueryPort.findPhotosWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                coupleId = coupleId,
                albumId = albumId,
            )
        return MapPhotosReadModel(photos = MapPhotos.of(photos.map { it.toMapPhotoReadModel() }))
    }
}
