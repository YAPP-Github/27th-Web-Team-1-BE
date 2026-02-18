package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import kotlin.math.floor

@Deprecated(
    message = "Distance 기반 전략은 더 이상 기본으로 사용되지 않습니다. PixelBasedClusterBoundaryMergeStrategy 사용을 권장합니다.",
    replaceWith = ReplaceWith("PixelBasedClusterBoundaryMergeStrategy()"),
)
class DistanceBasedClusterBoundaryMergeStrategy : ClusterBoundaryMergeStrategy {
    private val delegate = PixelBasedClusterBoundaryMergeStrategy()

    override fun mergeClusters(
        clusters: List<ClusterReadModel>,
        zoom: Double,
    ): List<ClusterReadModel> = delegate.mergeClusters(clusters, zoom)

    override fun resolveClusterCells(
        zoom: Int,
        photosByCell: Map<CellCoord, List<GeoPoint>>,
        targetCell: CellCoord,
    ): Set<CellCoord> = delegate.resolveClusterCells(zoom, photosByCell, targetCell)

    internal fun getGridSizeAtZoomLevel(zoom: Double): Double {
        val discrete = floor(zoom).toInt()
        return GridValues.getGridSize(discrete)
    }

    internal fun getBoundaryMergeEpsMeters(zoom: Double): Double = getGridSizeAtZoomLevel(zoom)
}
