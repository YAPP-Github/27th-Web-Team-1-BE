package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
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
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
    private val cacheManager: CacheManager,
) {
    data class CachedCell(
        val response: ClusterResponse?,
    )

    private fun lonToM(lon: Double): Double = lon * (PI * EARTH_RADIUS / 180.0)

    private fun latToM(lat: Double): Double = ln(tan((90.0 + lat) * PI / 360.0)) * EARTH_RADIUS

    fun evictForCouple(coupleId: Long) {
        coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
    }

    fun getVersion(coupleId: Long?): Long = coupleId?.let { coupleVersions[it]?.get() } ?: 0L

    fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom)
        val cache =
            cacheManager.getCache("mapCells") as? CaffeineCache
                ?: return queryDirectly(zoom, bbox, gridSize, coupleId, albumId)
        val version = getVersion(coupleId)

        val cellXMin = floor(lonToM(bbox.west) / gridSize).toLong()
        val cellXMax = floor(lonToM(bbox.east) / gridSize).toLong()
        val cellYMin = floor(latToM(bbox.south) / gridSize).toLong()
        val cellYMax = floor(latToM(bbox.north) / gridSize).toLong()

        val keyToCoord = mutableMapOf<String, Pair<Long, Long>>()
        for (cx in cellXMin..cellXMax) {
            for (cy in cellYMin..cellYMax) {
                val key = buildCellKey(zoom, cx, cy, coupleId, albumId, version)
                keyToCoord[key] = cx to cy
            }
        }

        if (keyToCoord.size > MAX_CELLS_FOR_CELL_CACHE) {
            return queryDirectly(zoom, bbox, gridSize, coupleId, albumId)
        }

        return StructuredConcurrency.run { scope ->
            val cachedTask =
                scope.fork {
                    cache.nativeCache.getAllPresent(keyToCoord.keys)
                }

            scope.join().throwIfFailed()

            val cachedMap = cachedTask.get()
            val cachedResponses = cachedMap.values.mapNotNull { (it as CachedCell).response }
            val uncachedKeys = keyToCoord.keys.filter { !cachedMap.containsKey(it) }

            if (uncachedKeys.isEmpty()) {
                return@run MapPhotosResponse(clusters = cachedResponses)
            }

            val uncachedCoords = uncachedKeys.mapNotNull { keyToCoord[it] }
            val dbResultsTask =
                scope.fork {
                    mapQueryPort.findClustersWithinBBox(
                        west = uncachedCoords.minOf { it.first } * gridSize,
                        south = uncachedCoords.minOf { it.second } * gridSize,
                        east = (uncachedCoords.maxOf { it.first } + 1) * gridSize,
                        north = (uncachedCoords.maxOf { it.second } + 1) * gridSize,
                        gridSize = gridSize,
                        coupleId = coupleId,
                        albumId = albumId,
                    )
                }

            scope.join().throwIfFailed()

            val dbResults = dbResultsTask.get()
            val dbCellMap =
                dbResults
                    .groupBy { it.cellX to it.cellY }
                    .mapValues { (_, projections) ->
                        val latest = projections.maxByOrNull { it.takenAt ?: LocalDateTime.MIN }!!
                        ClusterProjection(
                            cellX = projections.first().cellX,
                            cellY = projections.first().cellY,
                            count = projections.first().count,
                            thumbnailUrl = latest.thumbnailUrl,
                            centerLongitude = projections.map { it.centerLongitude }.average(),
                            centerLatitude = projections.map { it.centerLatitude }.average(),
                            takenAt = latest.takenAt,
                        )
                    }
            val newResponses = mutableListOf<ClusterResponse>()
            val bulkInsertMap = mutableMapOf<String, CachedCell>()

            for (key in uncachedKeys) {
                val coord = keyToCoord[key] ?: continue
                val projection = dbCellMap[coord.first to coord.second]
                val response = projection?.toResponse(zoom)

                bulkInsertMap[key] = CachedCell(response)
                response?.let { newResponses.add(it) }
            }

            scope.fork {
                cache.nativeCache.putAll(bulkInsertMap)
            }

            MapPhotosResponse(clusters = cachedResponses + newResponses)
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key =
            "T(kr.co.lokit.api.domain.map.application.MapPhotosCacheServiceKt)" +
                ".buildIndividualKey(#bbox, #zoom, #coupleId, #albumId, @mapPhotosCacheService.getVersion(#coupleId))",
        unless = "#result.photos == null || #result.photos.isEmpty()",
    )
    fun getIndividualPhotos(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
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
        return MapPhotosResponse(photos = photos.map { it.toMapPhotoResponse() })
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
                west = lonToM(bbox.west),
                south = latToM(bbox.south),
                east = lonToM(bbox.east),
                north = latToM(bbox.north),
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
            )
        return MapPhotosResponse(clusters = clusters.map { it.toResponse(zoom) })
    }

    fun buildCellKey(
        zoom: Int,
        cx: Long,
        cy: Long,
        cid: Long?,
        aid: Long?,
        v: Long,
    ): String = "z${zoom}_x${cx}_y${cy}_c${cid ?: 0}_a${aid ?: 0}_v$v"

    companion object {
        private const val MAX_CELLS_FOR_CELL_CACHE = 500
        private val coupleVersions = ConcurrentHashMap<Long, AtomicLong>()
        private const val EARTH_RADIUS = 6378137.0
    }
}

fun buildIndividualKey(
    bbox: BBox,
    zoom: Int,
    cid: Long?,
    aid: Long?,
    v: Long,
): String {
    val gs = GridValues.getGridSize(zoom)
    val nx = (floor(bbox.west * 1000000 / gs)).toLong()
    return "ind_z${zoom}_x${nx}_c${cid ?: 0}_v$v"
}
