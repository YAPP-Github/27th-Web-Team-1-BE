package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterReadModel

data class CellCoord(
    val x: Long,
    val y: Long,
)

data class GeoPoint(
    val longitude: Double,
    val latitude: Double,
    val weight: Int = 1,
)

interface ClusterBoundaryMergeStrategy {
    fun mergeClusters(
        clusters: List<ClusterReadModel>,
        zoom: Int,
    ): List<ClusterReadModel> = mergeClusters(clusters, zoom.toDouble())

    fun mergeClusters(
        clusters: List<ClusterReadModel>,
        zoom: Double,
    ): List<ClusterReadModel>

    fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord>
}
