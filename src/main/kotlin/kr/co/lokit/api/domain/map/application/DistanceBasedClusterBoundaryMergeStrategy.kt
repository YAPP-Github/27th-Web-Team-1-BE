package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.tan

class DistanceBasedClusterBoundaryMergeStrategy : ClusterBoundaryMergeStrategy {
    private val earthRadius = 6378137.0

    override fun mergeClusters(
        clusters: List<ClusterResponse>,
        zoom: Int,
    ): List<ClusterResponse> {
        if (clusters.size < 2) {
            return clusters
        }
        val gridSize = GridValues.getGridSize(zoom)
        val epsMeters = getBoundaryMergeEpsMeters(zoom, gridSize)
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
            val dominant = nodes.maxByOrNull { it.count } ?: representative
            ClusterResponse(
                clusterId = ClusterId.format(zoom, representative.cellX, representative.cellY),
                count = totalCount,
                thumbnailUrl = dominant.thumbnailUrl,
                longitude = if (totalCount > 0) sumLon / totalCount else representative.longitude,
                latitude = if (totalCount > 0) sumLat / totalCount else representative.latitude,
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
        val epsMeters = getBoundaryMergeEpsMeters(zoom, gridSize)

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
                if (planarDistanceMeters(a.longitude, a.latitude, b.longitude, b.latitude) <= thresholdMeters) {
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

    private fun getBoundaryMergeEpsMeters(
        zoom: Int,
        gridSize: Double,
    ): Double {
        val ratio =
            when {
                zoom <= 10 -> 0.16
                zoom == 11 -> 0.20
                zoom == 12 -> 0.18
                zoom == 13 -> 0.16
                else -> 0.14
            }
        return (gridSize * ratio).coerceIn(120.0, 900.0)
    }

    private fun lonToM(lon: Double): Double = lon * (PI * earthRadius / 180.0)

    private fun latToM(lat: Double): Double = ln(tan((90.0 + lat) * PI / 360.0)) * earthRadius

    private fun planarDistanceMeters(
        lonA: Double,
        latA: Double,
        lonB: Double,
        latB: Double,
    ): Double {
        val ax = lonToM(lonA)
        val ay = latToM(latA)
        val bx = lonToM(lonB)
        val by = latToM(latB)
        val dx = ax - bx
        val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    private data class MergeNode(
        val cellX: Long,
        val cellY: Long,
        val count: Int,
        val thumbnailUrl: String,
        val longitude: Double,
        val latitude: Double,
    )
}
