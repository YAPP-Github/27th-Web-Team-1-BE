package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DistanceBasedClusterBoundaryMergeStrategy : ClusterBoundaryMergeStrategy {
    override fun mergeClusters(
        clusters: List<ClusterResponse>,
        zoomLevel: Double,
    ): List<ClusterResponse> {
        if (clusters.size < 2) {
            return clusters
        }
        val normalizedZoomLevel = normalizeZoomLevel(zoomLevel)
        val zoom = floor(normalizedZoomLevel).toInt()
        val epsMeters = getBoundaryMergeEpsMeters(normalizedZoomLevel)
        val parsed =
            clusters.mapNotNull { cluster ->
                runCatching { ClusterId.parse(cluster.clusterId) }
                    .getOrNull()
                    ?.let { cell ->
                        MergeNode(
                            cellX = cell.cellX,
                            cellY = cell.cellY,
                            count = cluster.count,
                            thumbnailUrl = cluster.thumbnailUrl,
                            longitude = cluster.longitude,
                            latitude = cluster.latitude,
                            takenAt = cluster.takenAt,
                        )
                    }
            }
        if (parsed.size < 2) {
            return clusters
        }

        val groups =
            buildGroups(
                nodes = parsed,
                thresholdMeters = epsMeters,
                cellExtractor = { CellCoord(it.cellX, it.cellY) },
                coordExtractor = { GeoPoint(it.longitude, it.latitude, it.count) },
            )

        return groups.map { group ->
            val nodes = group.map { parsed[it] }
            val representative =
                nodes.minWith(
                    compareBy<MergeNode> { it.cellY }
                        .thenBy { it.cellX },
                )
            val totalCount = nodes.sumOf { it.count }
            val sumLon = nodes.sumOf { it.longitude * it.count }
            val sumLat = nodes.sumOf { it.latitude * it.count }
            val latestTakenAtNode = nodes.maxByOrNull { it.takenAt ?: LocalDateTime.MIN } ?: representative
            ClusterResponse(
                clusterId = ClusterId.format(zoom, representative.cellX, representative.cellY),
                count = totalCount,
                thumbnailUrl = latestTakenAtNode.thumbnailUrl,
                longitude = if (totalCount > 0) sumLon / totalCount else representative.longitude,
                latitude = if (totalCount > 0) sumLat / totalCount else representative.latitude,
                takenAt = latestTakenAtNode.takenAt,
            )
        }
    }

    override fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord> {
        if (photosByCell.size < 2) {
            return if (targetCell in photosByCell.keys) setOf(targetCell) else emptySet()
        }
        val cells = photosByCell.keys.toList()
        val cellCenters =
            cells.associateWith { cell ->
                val points = photosByCell[cell].orEmpty()
                val totalWeight = points.sumOf { it.weight }.coerceAtLeast(1)
                val lon = points.sumOf { it.longitude * it.weight } / totalWeight
                val lat = points.sumOf { it.latitude * it.weight } / totalWeight
                GeoPoint(lon, lat, totalWeight)
            }
        val gridSize = GridValues.getGridSize(zoom)
        val epsMeters = getBoundaryMergeEpsMeters(zoom.toDouble())

        val groups =
            buildGroups(cells, epsMeters) { cell ->
                cellCenters[cell] ?: GeoPoint(0.0, 0.0)
            }
        val idxByCell = cells.withIndex().associate { it.value to it.index }
        val targetIdx = idxByCell[targetCell] ?: return setOf(targetCell)
        val matched = groups.firstOrNull { targetIdx in it } ?: return setOf(targetCell)
        return matched.map { cells[it] }.toSet()
    }

    private fun <T> buildGroups(
        nodes: List<T>,
        thresholdMeters: Double,
        cellExtractor: (T) -> CellCoord,
        coordExtractor: (T) -> GeoPoint,
    ): List<List<Int>> {
        val parent = IntArray(nodes.size) { it }

        fun find(x: Int): Int {
            var cur = x
            while (parent[cur] != cur) {
                parent[cur] = parent[parent[cur]]
                cur = parent[cur]
            }
            return cur
        }

        fun union(
            a: Int,
            b: Int,
        ) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) {
                parent[rb] = ra
            }
        }

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val aCell = cellExtractor(nodes[i])
                val bCell = cellExtractor(nodes[j])
                if (abs(aCell.x - bCell.x) > 1L || abs(aCell.y - bCell.y) > 1L) {
                    continue
                }
                val a = coordExtractor(nodes[i])
                val b = coordExtractor(nodes[j])
                if (haversineDistanceMeters(a.longitude, a.latitude, b.longitude, b.latitude) <= thresholdMeters) {
                    union(i, j)
                }
            }
        }
        return nodes.indices
            .groupBy { find(it) }
            .values
            .map { it.toList() }
    }

    private fun buildGroups(
        cells: List<CellCoord>,
        thresholdMeters: Double,
        centerResolver: (CellCoord) -> GeoPoint,
    ): List<List<Int>> = buildGroups(cells, thresholdMeters, { it }, centerResolver)

    internal fun getBoundaryMergeEpsMeters(zoomLevel: Double): Double {
        val gridSize = getGridSizeAtZoomLevel(zoomLevel)
        val metersPerPixel = gridSize / GRID_PIXELS
        val thresholdPx = getPoiOverlapThresholdPx()
        return thresholdPx * metersPerPixel
    }

    private fun getPoiOverlapThresholdPx(): Double {
        val poiWidth = BASE_LIBRARY_POI_WIDTH_PX * POI_WIDTH_MULTIPLIER
        val poiHeight = BASE_LIBRARY_POI_HEIGHT_PX * POI_HEIGHT_MULTIPLIER
        val overlapThresholdX = poiWidth * (1.0 - REQUIRED_OVERLAP_RATIO)
        val overlapThresholdY = poiHeight * (1.0 - REQUIRED_OVERLAP_RATIO)
        return hypot(overlapThresholdX, overlapThresholdY)
    }

    internal fun getGridSizeAtZoomLevel(zoomLevel: Double): Double {
        val normalizedZoomLevel = normalizeZoomLevel(zoomLevel)
        val lowerZoom = floor(normalizedZoomLevel).toInt()
        val upperZoom = (lowerZoom + 1).coerceAtMost(MAX_ZOOM_LEVEL)
        if (lowerZoom == upperZoom) {
            return GridValues.getGridSize(lowerZoom, GRID_PIXELS)
        }
        val progress = normalizedZoomLevel - lowerZoom
        val lowerGrid = GridValues.getGridSize(lowerZoom, GRID_PIXELS)
        val upperGrid = GridValues.getGridSize(upperZoom, GRID_PIXELS)
        return lowerGrid * (upperGrid / lowerGrid).pow(progress)
    }

    private fun normalizeZoomLevel(zoomLevel: Double): Double = zoomLevel.coerceIn(0.0, MAX_ZOOM_LEVEL.toDouble())

    private fun toRad(degree: Double): Double = degree * PI / 180.0

    private fun haversineDistanceMeters(
        lonA: Double,
        latA: Double,
        lonB: Double,
        latB: Double,
    ): Double {
        val latARad = toRad(latA)
        val latBRad = toRad(latB)
        val dLat = toRad(latB - latA)
        val dLon = toRad(lonB - lonA)

        val a =
            sin(dLat / 2.0) * sin(dLat / 2.0) +
                cos(latARad) * cos(latBRad) *
                sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return MercatorProjection.EARTH_RADIUS_METERS * c
    }

    private data class MergeNode(
        val cellX: Long,
        val cellY: Long,
        val count: Int,
        val thumbnailUrl: String,
        val longitude: Double,
        val latitude: Double,
        val takenAt: LocalDateTime?,
    )

    companion object {
        private const val MAX_ZOOM_LEVEL = 22
        private const val GRID_PIXELS = 60
        private const val BASE_LIBRARY_POI_WIDTH_PX = 12.0
        private const val BASE_LIBRARY_POI_HEIGHT_PX = 12.0
        private const val POI_WIDTH_MULTIPLIER = 4.0
        private const val POI_HEIGHT_MULTIPLIER = 5.0
        private const val REQUIRED_OVERLAP_RATIO = 1.0 / 3.0
    }
}
