package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class PixelBasedClusterBoundaryMergeStrategy : ClusterBoundaryMergeStrategy {
    override fun mergeClusters(
        clusters: List<ClusterResponse>,
        zoomLevel: Double,
    ): List<ClusterResponse> {
        if (clusters.size < 2) return clusters

        val z = normalizeZoom(zoomLevel)
        val zoomDiscrete = floor(z).toInt()

        val parsed = mutableListOf<MergeNode>()
        val passthrough = mutableListOf<ClusterResponse>()

        clusters.forEach { cluster ->
            val parsedId =
                runCatching { ClusterId.parse(cluster.clusterId) }
                    .getOrNull()

            if (parsedId == null) {
                passthrough += cluster
            } else {
                parsed +=
                    MergeNode(
                        cellX = parsedId.cellX,
                        cellY = parsedId.cellY,
                        count = cluster.count,
                        thumbnailUrl = cluster.thumbnailUrl,
                        longitude = cluster.longitude,
                        latitude = cluster.latitude,
                        takenAt = cluster.takenAt,
                    )
            }
        }

        if (parsed.size < 2) return clusters

        val sortedParsed =
            parsed.sortedWith(
                compareBy<MergeNode> { it.cellY }
                    .thenBy { it.cellX }
                    .thenBy { it.latitude }
                    .thenBy { it.longitude },
            )

        val groups =
            buildGroupsByCompleteLinkage(
                zoomLevel = z,
                nodes = sortedParsed,
                lonLatExtractor = { it.longitude to it.latitude },
            )

        val merged =
            groups.map { group ->
                val nodes = group.map { sortedParsed[it] }

                val representative =
                    nodes.minWith(
                        compareBy<MergeNode> { it.cellY }
                            .thenBy { it.cellX },
                    )

                val totalCount = nodes.sumOf { it.count }
                val sumLon = nodes.sumOf { it.longitude * it.count }
                val sumLat = nodes.sumOf { it.latitude * it.count }

                val latestTakenAtNode =
                    nodes.maxByOrNull { it.takenAt ?: LocalDateTime.MIN } ?: representative

                ClusterResponse(
                    clusterId = ClusterId.format(zoomDiscrete, representative.cellX, representative.cellY),
                    count = totalCount,
                    thumbnailUrl = latestTakenAtNode.thumbnailUrl,
                    longitude = if (totalCount > 0) sumLon / totalCount else representative.longitude,
                    latitude = if (totalCount > 0) sumLat / totalCount else representative.latitude,
                    takenAt = latestTakenAtNode.takenAt,
                )
            }

        return ensureUniqueClusterIds(merged) + passthrough
    }

    override fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord> {
        if (photosByCell.size < 2) {
            return if (targetCell in photosByCell.keys) setOf(targetCell) else emptySet()
        }

        val cells =
            photosByCell.keys
                .sortedWith(
                    compareBy<CellCoord> { it.y }
                        .thenBy { it.x },
                )

        val cellCenters =
            cells.associateWith { cell ->
                val points = photosByCell[cell].orEmpty()
                val totalWeight = points.sumOf { it.weight }.coerceAtLeast(1)
                val lon = points.sumOf { it.longitude * it.weight } / totalWeight
                val lat = points.sumOf { it.latitude * it.weight } / totalWeight
                GeoPoint(lon, lat, totalWeight)
            }

        val z = zoom.toDouble()

        val groups =
            buildGroupsByCompleteLinkage(
                zoomLevel = z,
                nodes = cells,
                lonLatExtractor = { cell ->
                    val center = cellCenters[cell] ?: GeoPoint(0.0, 0.0)
                    center.longitude to center.latitude
                },
            )

        val idxByCell = cells.withIndex().associate { it.value to it.index }
        val targetIdx = idxByCell[targetCell] ?: return setOf(targetCell)
        val matched = groups.firstOrNull { targetIdx in it } ?: return setOf(targetCell)

        return matched.map { cells[it] }.toSet()
    }

    private fun lonLatToWorldPx(
        lon: Double,
        lat: Double,
        zoom: Double,
    ): Pair<Double, Double> {
        val worldSize = 256.0 * 2.0.pow(zoom)
        val x = (lon + 180.0) / 360.0 * worldSize

        val siny = sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        val y = (0.5 - ln((1 + siny) / (1 - siny)) / (4 * Math.PI)) * worldSize

        return x to y
    }

    private fun <T> buildGroupsByCompleteLinkage(
        zoomLevel: Double,
        nodes: List<T>,
        lonLatExtractor: (T) -> Pair<Double, Double>,
    ): List<List<Int>> {
        if (nodes.isEmpty()) return emptyList()

        val projected =
            nodes.map { n ->
                val (lon, lat) = lonLatExtractor(n)
                val (x, y) = lonLatToWorldPx(lon, lat, zoomLevel)
                ProjectedNode(x, y)
            }

        val dsu = CompleteLinkageDsu(projected)
        val edges = buildCandidateEdges(projected)

        edges.forEach { edge ->
            dsu.unionIfCompleteLinkageSafe(edge.a, edge.b)
        }

        val initialGroups =
            nodes.indices
                .groupBy { dsu.find(it) }
                .values
                .map { it.toList() }
        return initialGroups
    }

    private fun buildCandidateEdges(nodes: List<ProjectedNode>): List<Edge> {
        val bucketMap = mutableMapOf<BucketKey, MutableList<Int>>()

        nodes.forEachIndexed { index, node ->
            val key = BucketKey(floor(node.x / MERGE_DX_PX).toLong(), floor(node.y / MERGE_DY_PX).toLong())
            bucketMap.getOrPut(key) { mutableListOf() }.add(index)
        }

        val edges = mutableListOf<Edge>()
        nodes.forEachIndexed { i, a ->
            val bx = floor(a.x / MERGE_DX_PX).toLong()
            val by = floor(a.y / MERGE_DY_PX).toLong()

            for (dxBucket in -1L..1L) {
                for (dyBucket in -1L..1L) {
                    val neighbor = BucketKey(bx + dxBucket, by + dyBucket)
                    val candidates = bucketMap[neighbor] ?: continue
                    candidates.forEach { j ->
                        if (j <= i) return@forEach
                        val b = nodes[j]
                        val dx = abs(a.x - b.x)
                        if (dx >= MERGE_DX_PX) return@forEach
                        val dy = abs(a.y - b.y)
                        if (dy >= MERGE_DY_PX) return@forEach
                        val score = max(dx / MERGE_DX_PX, dy / MERGE_DY_PX)
                        edges += Edge(a = i, b = j, score = score, dx = dx, dy = dy)
                    }
                }
            }
        }

        return edges.sortedWith(
            compareBy<Edge> { it.score }
                .thenBy { it.dx }
                .thenBy { it.dy }
                .thenBy { it.a }
                .thenBy { it.b },
        )
    }

    private fun normalizeZoom(zoomLevel: Double): Double = zoomLevel.coerceIn(0.0, MAX_ZOOM_LEVEL.toDouble())

    private fun ensureUniqueClusterIds(clusters: List<ClusterResponse>): List<ClusterResponse> {
        val seen = mutableMapOf<String, Int>()
        return clusters.map { cluster ->
            val seq = (seen[cluster.clusterId] ?: 0) + 1
            seen[cluster.clusterId] = seq
            if (seq == 1) {
                cluster
            } else {
                cluster.copy(clusterId = "${cluster.clusterId}_g$seq")
            }
        }
    }

    private class CompleteLinkageDsu(
        nodes: List<ProjectedNode>,
    ) {
        private val parent = IntArray(nodes.size) { it }
        private val size = IntArray(nodes.size) { 1 }
        private val minX = DoubleArray(nodes.size) { index -> nodes[index].x }
        private val maxX = DoubleArray(nodes.size) { index -> nodes[index].x }
        private val minY = DoubleArray(nodes.size) { index -> nodes[index].y }
        private val maxY = DoubleArray(nodes.size) { index -> nodes[index].y }

        fun find(x: Int): Int {
            var cur = x
            while (parent[cur] != cur) {
                parent[cur] = parent[parent[cur]]
                cur = parent[cur]
            }
            return cur
        }

        fun unionIfCompleteLinkageSafe(
            a: Int,
            b: Int,
        ) {
            var ra = find(a)
            var rb = find(b)
            if (ra == rb) return

            val combinedMinX = min(minX[ra], minX[rb])
            val combinedMaxX = max(maxX[ra], maxX[rb])
            val combinedMinY = min(minY[ra], minY[rb])
            val combinedMaxY = max(maxY[ra], maxY[rb])

            val spanX = combinedMaxX - combinedMinX
            val spanY = combinedMaxY - combinedMinY
            if (spanX > MERGE_DX_PX || spanY > MERGE_DY_PX) return

            if (size[ra] < size[rb]) {
                val tmp = ra
                ra = rb
                rb = tmp
            }

            parent[rb] = ra
            size[ra] += size[rb]
            minX[ra] = combinedMinX
            maxX[ra] = combinedMaxX
            minY[ra] = combinedMinY
            maxY[ra] = combinedMaxY
        }
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

    private data class ProjectedNode(
        val x: Double,
        val y: Double,
    )

    private data class BucketKey(
        val x: Long,
        val y: Long,
    )

    private data class Edge(
        val a: Int,
        val b: Int,
        val score: Double,
        val dx: Double,
        val dy: Double,
    )

    companion object {
        private const val MAX_ZOOM_LEVEL = 22

        private const val POI_WIDTH_PX = 74.0
        private const val POI_HEIGHT_PX = 100.0
        private const val REQUIRED_OVERLAP_RATIO = 1.0 / 3.0

        private const val EXTRA_CLOSENESS_PX_X = 17.0
        private const val EXTRA_CLOSENESS_PX_Y = 18.0
        private const val MERGE_DX_PX = ((1.0 - REQUIRED_OVERLAP_RATIO) * POI_WIDTH_PX) - EXTRA_CLOSENESS_PX_X
        private const val MERGE_DY_PX = ((1.0 - REQUIRED_OVERLAP_RATIO) * POI_HEIGHT_PX) - EXTRA_CLOSENESS_PX_Y
    }
}
