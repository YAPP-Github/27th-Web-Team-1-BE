package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterReadModel

@Deprecated(
    message = "Legacy 전략은 더 이상 기본으로 사용되지 않습니다. PixelBasedClusterBoundaryMergeStrategy 사용을 권장합니다.",
    replaceWith = ReplaceWith("PixelBasedClusterBoundaryMergeStrategy()"),
)
class LegacyClusterBoundaryMergeStrategy : ClusterBoundaryMergeStrategy {
    override fun mergeClusters(
        clusters: List<ClusterReadModel>,
        zoom: Double,
    ): List<ClusterReadModel> = clusters

    override fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord> = if (targetCell in photosByCell.keys) setOf(targetCell) else emptySet()
}
