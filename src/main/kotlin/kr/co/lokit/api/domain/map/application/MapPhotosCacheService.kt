package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import kr.co.lokit.api.common.util.orZero
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
    private val cacheManager: CacheManager,
    private val clusterBoundaryMergeStrategy: ClusterBoundaryMergeStrategy,
) {
    data class CachedCell(
        val response: ClusterResponse?,
    )

    private fun lonToM(lon: Double): Double = MercatorProjection.longitudeToMeters(lon)

    private fun latToM(lat: Double): Double = MercatorProjection.latitudeToMeters(lat)

    private data class ViewportState(
        val centerX: Long,
        val centerY: Long,
        val requestedAtMillis: Long,
    )

    private data class PhotoMutation(
        val sequence: Long,
        val longitude: Double,
        val latitude: Double,
        val albumId: Long?,
    )

    fun evictForCouple(coupleId: Long) {
        coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
        evictCoupleEntries(CacheNames.MAP_CELLS, coupleId)
        evictCoupleEntries(CacheNames.MAP_PHOTOS, coupleId)
        latestCellVersionByBaseKey.keys.removeIf { it.contains("_c${coupleId}_") }
        requestStates.keys.removeIf { it.contains("_c${coupleId}_") }
        coupleMutations.remove(coupleId)
    }

    fun getVersion(coupleId: Long?): Long = coupleId?.let { coupleVersions[it]?.get() }.orZero()

    fun getVersion(
        _zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Long {
        if (coupleId == null) {
            return 0L
        }
        val mutations = coupleMutations[coupleId] ?: return 0L
        var maxSequence = 0L
        for (mutation in mutations) {
            if (mutation.longitude < bbox.west || mutation.longitude > bbox.east) {
                continue
            }
            if (mutation.latitude < bbox.south || mutation.latitude > bbox.north) {
                continue
            }
            if (albumId != null && mutation.albumId != albumId) {
                continue
            }
            if (mutation.sequence > maxSequence) {
                maxSequence = mutation.sequence
            }
        }
        return maxSequence
    }

    fun getDataVersion(
        _zoom: Int,
        _bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
    ): Long {
        if (coupleId == null) {
            return 0L
        }
        val mutationVersion = getAlbumScopedMutationVersion(coupleId, albumId)
        return (fnv64(coupleId, albumId.orZero(), mutationVersion) and Long.MAX_VALUE)
    }

    private fun fnv64(vararg values: Long): Long {
        var hash = FNV64_OFFSET_BASIS
        values.forEach { value ->
            hash = hash xor value
            hash *= FNV64_PRIME
        }
        return hash
    }

    fun evictForPhotoMutation(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ) {
        recordMutation(coupleId, albumId, longitude, latitude)
        evictCellEntriesForPoint(coupleId, albumId, longitude, latitude)
        evictIndividualEntriesForPoint(coupleId, albumId, longitude, latitude)
        requestStates.keys.removeIf { it.contains("_c${coupleId}_") }
    }

    fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        zoomLevel: Double = zoom.toDouble(),
        canReuseCellCache: Boolean = true,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom)
        val cache =
            cacheManager.getCache(CacheNames.MAP_CELLS) as? CaffeineCache
                ?: return queryDirectly(zoom, bbox, gridSize, coupleId, albumId, zoomLevel)
        val version = getVersion(zoom, bbox, coupleId, albumId)
        val sequence = getMutationSequence(coupleId)

        val cellXMin = floor(lonToM(bbox.west) / gridSize).toLong()
        val cellXMax = floor(lonToM(bbox.east) / gridSize).toLong()
        val cellYMin = floor(latToM(bbox.south) / gridSize).toLong()
        val cellYMax = floor(latToM(bbox.north) / gridSize).toLong()

        val requestedCoords = LinkedHashSet<Pair<Long, Long>>()
        val keyToCoord = mutableMapOf<String, Pair<Long, Long>>()
        for (cx in cellXMin..cellXMax) {
            for (cy in cellYMin..cellYMax) {
                requestedCoords.add(cx to cy)
                val key = MapCacheKeyFactory.buildCellKey(zoom, cx, cy, coupleId, albumId, version)
                keyToCoord[key] = cx to cy
            }
        }

        if (keyToCoord.size > MAX_CELLS_FOR_CELL_CACHE) {
            return queryDirectly(zoom, bbox, gridSize, coupleId, albumId, zoomLevel)
        }

        val prefetchCoordToKey =
            calculateDirectionalPrefetchCells(
                zoom = zoom,
                gridSize = gridSize,
                requestedXMin = cellXMin,
                requestedXMax = cellXMax,
                requestedYMin = cellYMin,
                requestedYMax = cellYMax,
                bbox = bbox,
                coupleId = coupleId,
                albumId = albumId,
                version = version,
            )

        val (cachedCellsByCoord, uncachedCoords) =
            if (canReuseCellCache) {
                val cachedMap = cache.nativeCache.getAllPresent(keyToCoord.keys)
                val cachedByCoord =
                    cachedMap.entries
                        .mapNotNull { entry ->
                            val key = entry.key as? String ?: return@mapNotNull null
                            val coord = keyToCoord[key] ?: return@mapNotNull null
                            val cell = entry.value as? CachedCell ?: return@mapNotNull null
                            coord to cell
                        }.toMap()
                cachedByCoord.keys.forEach { coord ->
                    latestCellVersionByBaseKey[MapCacheKeyFactory.buildCellBaseKey(zoom, coord.first, coord.second, coupleId, albumId)] =
                        version
                }
                cachedByCoord to (requestedCoords - cachedByCoord.keys)
            } else {
                resolveReusableCellsOnVersionMismatch(
                    cache = cache,
                    zoom = zoom,
                    gridSize = gridSize,
                    coupleId = coupleId,
                    albumId = albumId,
                    requestedCoords = requestedCoords,
                )
            }
        val cachedResponses = cachedCellsByCoord.values.mapNotNull { it.response }

        if (uncachedCoords.isEmpty()) {
            schedulePrefetch(
                cache = cache,
                zoom = zoom,
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
                sequence = sequence,
                prefetchCoordToKey = prefetchCoordToKey,
            )
            return MapPhotosResponse(clusters = clusterBoundaryMergeStrategy.mergeClusters(cachedResponses, zoomLevel))
        }

        val dbCellMap = fetchClusterCellMap(uncachedCoords, gridSize, coupleId, albumId)
        val newResponses = mutableListOf<ClusterResponse>()
        val bulkInsertMap = mutableMapOf<String, CachedCell>()

        for (coord in uncachedCoords) {
            val key = MapCacheKeyFactory.buildCellKey(zoom, coord.first, coord.second, coupleId, albumId, version)
            val projection = dbCellMap[coord.first to coord.second]
            val response = projection?.toResponse(zoom)
            bulkInsertMap[key] = CachedCell(response)
            latestCellVersionByBaseKey[MapCacheKeyFactory.buildCellBaseKey(zoom, coord.first, coord.second, coupleId, albumId)] =
                version
            response?.let { newResponses.add(it) }
        }

        if (bulkInsertMap.isNotEmpty()) {
            cache.nativeCache.putAll(bulkInsertMap)
        }
        schedulePrefetch(
            cache = cache,
            zoom = zoom,
            gridSize = gridSize,
            coupleId = coupleId,
            albumId = albumId,
            sequence = sequence,
            prefetchCoordToKey = prefetchCoordToKey,
        )

        return MapPhotosResponse(
            clusters =
                clusterBoundaryMergeStrategy.mergeClusters(
                    cachedResponses + newResponses,
                    zoomLevel,
                ),
        )
    }

    private fun schedulePrefetch(
        cache: CaffeineCache,
        zoom: Int,
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
        sequence: Long,
        prefetchCoordToKey: Map<Pair<Long, Long>, String>,
    ) {
        if (prefetchCoordToKey.isEmpty()) {
            return
        }
        if (!prefetchSemaphore.tryAcquire()) {
            return
        }

        Thread.startVirtualThread {
            try {
                if (coupleId != null && getMutationSequence(coupleId) != sequence) {
                    return@startVirtualThread
                }
                val dbCellMap =
                    fetchClusterCellMap(
                        coords = prefetchCoordToKey.keys,
                        gridSize = gridSize,
                        coupleId = coupleId,
                        albumId = albumId,
                    )
                if (coupleId != null && getMutationSequence(coupleId) != sequence) {
                    return@startVirtualThread
                }

                val bulkInsertMap = mutableMapOf<String, CachedCell>()
                for ((coord, key) in prefetchCoordToKey) {
                    val projection = dbCellMap[coord.first to coord.second]
                    val response = projection?.toResponse(zoom)
                    bulkInsertMap[key] = CachedCell(response)
                    val parsed = MapCacheKeyFactory.parseCellKey(key)
                    if (parsed != null) {
                        val baseKey =
                            MapCacheKeyFactory.buildCellBaseKey(
                                zoom = parsed.zoom,
                                cellX = parsed.cellX,
                                cellY = parsed.cellY,
                                coupleId = parsed.coupleId,
                                albumId = parsed.albumId,
                            )
                        latestCellVersionByBaseKey[baseKey] = parsed.version
                    }
                }
                if (bulkInsertMap.isNotEmpty()) {
                    cache.nativeCache.putAll(bulkInsertMap)
                }
            } finally {
                prefetchSemaphore.release()
            }
        }
    }

    private fun fetchClusterCellMap(
        coords: Set<Pair<Long, Long>>,
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
    ): Map<Pair<Long, Long>, ClusterProjection> {
        if (coords.isEmpty()) {
            return emptyMap()
        }

        val dbResults =
            mapQueryPort.findClustersWithinBBox(
                west = coords.minOf { it.first } * gridSize,
                south = coords.minOf { it.second } * gridSize,
                east = (coords.maxOf { it.first } + 1) * gridSize,
                north = (coords.maxOf { it.second } + 1) * gridSize,
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
            )
        return dbResults
            .groupBy { it.cellX to it.cellY }
            .mapValues { (_, projections) ->
                val latest = projections.maxByOrNull { it.takenAt ?: LocalDateTime.MIN }!!
                ClusterProjection(
                    cellX = projections.first().cellX,
                    cellY = projections.first().cellY,
                    count = projections.sumOf { it.count },
                    thumbnailUrl = latest.thumbnailUrl,
                    centerLongitude = projections.map { it.centerLongitude }.average(),
                    centerLatitude = projections.map { it.centerLatitude }.average(),
                    takenAt = latest.takenAt,
                )
            }
    }

    private fun resolveReusableCellsOnVersionMismatch(
        cache: CaffeineCache,
        zoom: Int,
        gridSize: Double,
        coupleId: Long?,
        albumId: Long?,
        requestedCoords: Set<Pair<Long, Long>>,
    ): Pair<Map<Pair<Long, Long>, CachedCell>, Set<Pair<Long, Long>>> {
        val latestMutationByCell =
            calculateLatestMutationSequenceByCell(
                coupleId = coupleId,
                albumId = albumId,
                gridSize = gridSize,
                requestedCoords = requestedCoords,
            )
        val cachedByCoord = mutableMapOf<Pair<Long, Long>, CachedCell>()
        for (coord in requestedCoords) {
            val baseKey = MapCacheKeyFactory.buildCellBaseKey(zoom, coord.first, coord.second, coupleId, albumId)
            val cachedVersion = latestCellVersionByBaseKey[baseKey] ?: continue
            val requiredVersion = latestMutationByCell[coord].orZero()
            if (cachedVersion < requiredVersion) {
                continue
            }
            val versionedKey =
                MapCacheKeyFactory.buildCellKey(
                    zoom = zoom,
                    cellX = coord.first,
                    cellY = coord.second,
                    coupleId = coupleId,
                    albumId = albumId,
                    version = cachedVersion,
                )
            val cachedCell = cache.nativeCache.getIfPresent(versionedKey) as? CachedCell
            if (cachedCell != null) {
                cachedByCoord[coord] = cachedCell
                continue
            }
            latestCellVersionByBaseKey.remove(baseKey, cachedVersion)
        }
        val uncachedCoords = requestedCoords - cachedByCoord.keys
        return cachedByCoord to uncachedCoords
    }

    private fun calculateLatestMutationSequenceByCell(
        coupleId: Long?,
        albumId: Long?,
        gridSize: Double,
        requestedCoords: Set<Pair<Long, Long>>,
    ): Map<Pair<Long, Long>, Long> {
        if (coupleId == null || requestedCoords.isEmpty()) {
            return emptyMap()
        }
        val mutations = coupleMutations[coupleId] ?: return emptyMap()
        val latestByCell = HashMap<Pair<Long, Long>, Long>()
        for (mutation in mutations) {
            if (albumId != null && mutation.albumId != albumId) {
                continue
            }
            val coord =
                floor(lonToM(mutation.longitude) / gridSize).toLong() to
                    floor(latToM(mutation.latitude) / gridSize).toLong()
            if (coord !in requestedCoords) {
                continue
            }
            val current = latestByCell[coord].orZero()
            if (mutation.sequence > current) {
                latestByCell[coord] = mutation.sequence
            }
        }
        return latestByCell
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

    private fun recordMutation(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ) {
        val sequence = coupleVersions.computeIfAbsent(coupleId) { AtomicLong(0) }.incrementAndGet()
        val deque = coupleMutations.computeIfAbsent(coupleId) { ConcurrentLinkedDeque() }
        deque.addLast(
            PhotoMutation(
                sequence = sequence,
                longitude = longitude,
                latitude = latitude,
                albumId = albumId,
            ),
        )
        while (deque.size > MAX_MUTATIONS_PER_COUPLE) {
            deque.pollFirst()
        }
    }

    private fun getMutationSequence(coupleId: Long?): Long = getVersion(coupleId)

    private fun getAlbumScopedMutationVersion(
        coupleId: Long,
        albumId: Long?,
    ): Long {
        if (albumId == null) {
            return getMutationSequence(coupleId)
        }
        val mutations = coupleMutations[coupleId] ?: return 0L
        var maxSequence = 0L
        for (mutation in mutations) {
            if (mutation.albumId != albumId) {
                continue
            }
            if (mutation.sequence > maxSequence) {
                maxSequence = mutation.sequence
            }
        }
        return maxSequence
    }

    private fun evictCellEntriesForPoint(
        coupleId: Long,
        albumId: Long?,
        longitude: Double,
        latitude: Double,
    ) {
        val cache = cacheManager.getCache(CacheNames.MAP_CELLS) as? CaffeineCache ?: return
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
                val parsed = MapCacheKeyFactory.parseCellKey(key) ?: return@filter false
                if (parsed.coupleId != coupleId) return@filter false
                if (parsed.albumId != 0L && parsed.albumId != targetAlbum) return@filter false
                val gridSize = GridValues.getGridSize(parsed.zoom)
                val cellX = floor(lonToM(longitude) / gridSize).toLong()
                val cellY = floor(latToM(latitude) / gridSize).toLong()
                parsed.cellX == cellX && parsed.cellY == cellY
            }
        if (keysToInvalidate.isNotEmpty()) {
            cache.nativeCache.invalidateAll(keysToInvalidate)
            keysToInvalidate.forEach { key ->
                val parsed = MapCacheKeyFactory.parseCellKey(key) ?: return@forEach
                val baseKey =
                    MapCacheKeyFactory.buildCellBaseKey(
                        zoom = parsed.zoom,
                        cellX = parsed.cellX,
                        cellY = parsed.cellY,
                        coupleId = parsed.coupleId,
                        albumId = parsed.albumId,
                    )
                latestCellVersionByBaseKey.remove(baseKey)
            }
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

    private fun calculateDirectionalPrefetchCells(
        zoom: Int,
        gridSize: Double,
        requestedXMin: Long,
        requestedXMax: Long,
        requestedYMin: Long,
        requestedYMax: Long,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        version: Long,
    ): Map<Pair<Long, Long>, String> {
        if (coupleId == null) {
            return emptyMap()
        }

        val centerLon = (bbox.west + bbox.east) / 2.0
        val centerLat = (bbox.south + bbox.north) / 2.0
        val centerX = floor(lonToM(centerLon) / gridSize).toLong()
        val centerY = floor(latToM(centerLat) / gridSize).toLong()
        val now = System.currentTimeMillis()
        val stateKey = MapCacheKeyFactory.buildRequestStateKey(zoom, coupleId, albumId)
        val previous = requestStates.put(stateKey, ViewportState(centerX, centerY, now)) ?: return emptyMap()

        val deltaX = centerX - previous.centerX
        val deltaY = centerY - previous.centerY
        if (deltaX == 0L && deltaY == 0L) {
            return emptyMap()
        }

        val elapsedMillis = max(1L, now - previous.requestedAtMillis)
        val cellSpeedPerSec = (max(abs(deltaX), abs(deltaY)).toDouble() * 1000.0) / elapsedMillis
        val baseMarginByZoom =
            when {
                zoom <= 10 -> 4
                zoom <= 12 -> 3
                zoom <= 14 -> 2
                zoom <= 16 -> 1
                else -> 0
            }
        val speedBoost =
            when {
                cellSpeedPerSec >= FAST_PAN_CELLS_PER_SEC -> 2
                cellSpeedPerSec >= NORMAL_PAN_CELLS_PER_SEC -> 1
                else -> 0
            }
        val margin = min(MAX_PREFETCH_MARGIN_CELLS, baseMarginByZoom + speedBoost)
        if (margin <= 0) {
            return emptyMap()
        }

        val korea = BBox.KOREA_BOUNDS
        val koreaXMin = floor(lonToM(korea.west) / gridSize).toLong()
        val koreaXMax = floor(lonToM(korea.east) / gridSize).toLong()
        val koreaYMin = floor(latToM(korea.south) / gridSize).toLong()
        val koreaYMax = floor(latToM(korea.north) / gridSize).toLong()

        val coords = LinkedHashSet<Pair<Long, Long>>()
        val dirX = deltaX.compareTo(0L)
        val dirY = deltaY.compareTo(0L)

        if (dirX > 0) {
            for (x in (requestedXMax + 1)..(requestedXMax + margin)) {
                for (y in (requestedYMin - margin)..(requestedYMax + margin)) {
                    coords.add(x to y)
                }
            }
        } else if (dirX < 0) {
            for (x in (requestedXMin - margin)..(requestedXMin - 1)) {
                for (y in (requestedYMin - margin)..(requestedYMax + margin)) {
                    coords.add(x to y)
                }
            }
        }

        if (dirY > 0) {
            for (y in (requestedYMax + 1)..(requestedYMax + margin)) {
                for (x in (requestedXMin - margin)..(requestedXMax + margin)) {
                    coords.add(x to y)
                }
            }
        } else if (dirY < 0) {
            for (y in (requestedYMin - margin)..(requestedYMin - 1)) {
                for (x in (requestedXMin - margin)..(requestedXMax + margin)) {
                    coords.add(x to y)
                }
            }
        }

        return coords
            .asSequence()
            .filter { (x, y) ->
                x in koreaXMin..koreaXMax &&
                    y in koreaYMin..koreaYMax &&
                    (x !in requestedXMin..requestedXMax || y !in requestedYMin..requestedYMax)
            }.take(MAX_PREFETCH_CELLS)
            .associateWith { (x, y) -> MapCacheKeyFactory.buildCellKey(zoom, x, y, coupleId, albumId, version) }
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = [CacheNames.MAP_PHOTOS],
        key =
            "T(kr.co.lokit.api.domain.map.application.MapCacheKeyFactory)" +
                ".buildIndividualKey(#bbox, #zoom, #coupleId, #albumId, @mapPhotosCacheService.getVersion(#zoom, #bbox, #coupleId, #albumId))",
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
        zoomLevel: Double = zoom.toDouble(),
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
        return MapPhotosResponse(
            clusters =
                clusterBoundaryMergeStrategy.mergeClusters(
                    clusters.map { it.toResponse(zoom) },
                    zoomLevel,
                ),
        )
    }

    fun buildCellKey(
        zoom: Int,
        cx: Long,
        cy: Long,
        cid: Long?,
        aid: Long?,
        v: Long,
    ): String = MapCacheKeyFactory.buildCellKey(zoom, cx, cy, cid, aid, v)

    companion object {
        private const val MAX_CELLS_FOR_CELL_CACHE = 500
        private const val MAX_PREFETCH_CELLS = 120
        private const val MAX_PREFETCH_MARGIN_CELLS = 6
        private const val NORMAL_PAN_CELLS_PER_SEC = 1.5
        private const val FAST_PAN_CELLS_PER_SEC = 3.0
        private const val MAX_MUTATIONS_PER_COUPLE = 2000
        private val prefetchSemaphore = Semaphore(2)
        private val coupleVersions = ConcurrentHashMap<Long, AtomicLong>()
        private val coupleMutations = ConcurrentHashMap<Long, ConcurrentLinkedDeque<PhotoMutation>>()
        private val latestCellVersionByBaseKey = ConcurrentHashMap<String, Long>()
        private val requestStates = ConcurrentHashMap<String, ViewportState>()
        private const val FNV64_OFFSET_BASIS = -3750763034362895579L
        private const val FNV64_PRIME = 1099511628211L
    }
}
