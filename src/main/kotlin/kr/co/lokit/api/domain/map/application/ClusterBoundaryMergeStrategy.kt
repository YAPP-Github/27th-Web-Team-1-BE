package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.dto.ClusterResponse

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
        clusters: List<ClusterResponse>,
        zoom: Int,
    ): List<ClusterResponse>

    fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord>
}
