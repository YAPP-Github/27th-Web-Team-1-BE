package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.floor

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
    private val cacheManager: CacheManager,
) {
    data class CachedCell(
        val response: ClusterResponse?,
    )

    private val coupleVersions = ConcurrentHashMap<Long, AtomicLong>()

    fun getDataVersion(coupleId: Long?): Long = coupleId?.let { coupleVersions[it]?.get() ?: 0L } ?: 0L

    fun evictForCouple(coupleId: Long) {
        coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
        listOf("mapCells", "mapPhotos").forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName) as? CaffeineCache ?: return@forEach
            cache.nativeCache.asMap().keys.removeIf { key ->
                (key as String).contains("_c$coupleId")
            }
        }
    }

    /**
     * Grid cell 단위 캐싱으로 클러스터 데이터를 조회한다.
     * 1. bbox 내 모든 셀을 계산
     * 2. 캐시된 셀은 제외
     * 3. 미캐싱 셀만 DB에서 조회
     * 4. 결과를 합쳐서 반환
     */
    fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom - 1)
        val inverseGridSize = 1.0 / gridSize
        val cache = cacheManager.getCache("mapCells") as? CaffeineCache

        val cellXMin = floor(bbox.west * inverseGridSize).toLong()
        val cellXMax = floor(bbox.east * inverseGridSize).toLong()
        val cellYMin = floor(bbox.south * inverseGridSize).toLong()
        val cellYMax = floor(bbox.north * inverseGridSize).toLong()

        val totalCells = (cellXMax - cellXMin + 1) * (cellYMax - cellYMin + 1)

        if (totalCells > MAX_CELLS_FOR_CELL_CACHE || cache == null) {
            return queryDirectly(zoom, bbox, gridSize, coupleId, albumId)
        }

        val cachedResponses = mutableListOf<ClusterResponse>()
        val uncachedCells = mutableListOf<Pair<Long, Long>>()

        for (cx in cellXMin..cellXMax) {
            for (cy in cellYMin..cellYMax) {
                val key = buildCellKey(zoom, cx, cy, coupleId, albumId)
                val cached = cache.nativeCache.getIfPresent(key) as? CachedCell
                if (cached != null) {
                    cached.response?.let { cachedResponses.add(it) }
                } else {
                    uncachedCells.add(cx to cy)
                }
            }
        }

        if (uncachedCells.isEmpty()) {
            return MapPhotosResponse(clusters = cachedResponses)
        }

        // 미캐싱 셀의 최소 bbox 계산
        val uncachedWest = uncachedCells.minOf { it.first } * gridSize
        val uncachedSouth = uncachedCells.minOf { it.second } * gridSize
        val uncachedEast = (uncachedCells.maxOf { it.first } + 1) * gridSize
        val uncachedNorth = (uncachedCells.maxOf { it.second } + 1) * gridSize

        val dbResults =
            mapQueryPort.findClustersWithinBBox(
                west = uncachedWest,
                south = uncachedSouth,
                east = uncachedEast,
                north = uncachedNorth,
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
            )

        val dbCellMap = dbResults.associateBy { it.cellX to it.cellY }
        val newResponses = mutableListOf<ClusterResponse>()

        for ((cx, cy) in uncachedCells) {
            val key = buildCellKey(zoom, cx, cy, coupleId, albumId)
            val projection = dbCellMap[cx to cy]
            val response = projection?.toResponse(zoom)
            cache.nativeCache.put(key, CachedCell(response))
            if (response != null) newResponses.add(response)
        }

        return MapPhotosResponse(clusters = cachedResponses + newResponses)
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key = "#cacheKey",
        unless = "(#result.clusters == null || #result.clusters.isEmpty()) && (#result.photos == null || #result.photos.isEmpty())",
    )
    fun getIndividualPhotos(
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        cacheKey: String,
    ): MapPhotosResponse {
        val photos =
            mapQueryPort.findPhotosWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                coupleId = coupleId,
                albumId = albumId,
            )

        return MapPhotosResponse(
            photos = photos.map { it.toMapPhotoResponse() },
        )
    }

    fun buildCacheKey(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): String {
        val precision = 10000
        val w = floor(bbox.west * precision) / precision
        val s = floor(bbox.south * precision) / precision
        val e = floor(bbox.east * precision) / precision
        val n = floor(bbox.north * precision) / precision
        return buildString {
            append("z$zoom")
            append("_${w}_${s}_${e}_$n")
            if (coupleId != null) append("_c$coupleId")
            if (albumId != null) append("_a$albumId")
        }
    }

    private fun queryDirectly(
        zoom: Int,
        bbox: BBox,
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
    ): MapPhotosResponse {
        val clusters =
            mapQueryPort.findClustersWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
            )
        return MapPhotosResponse(clusters = clusters.map { it.toResponse(zoom) })
    }

    private fun buildCellKey(
        zoom: Int,
        cellX: Long,
        cellY: Long,
        coupleId: Long?,
        albumId: Long?,
    ): String =
        buildString {
            append("z${zoom}_${cellX}_$cellY")
            if (coupleId != null) append("_c$coupleId")
            if (albumId != null) append("_a$albumId")
        }

    companion object {
        private const val MAX_CELLS_FOR_CELL_CACHE = 5000
    }
}
