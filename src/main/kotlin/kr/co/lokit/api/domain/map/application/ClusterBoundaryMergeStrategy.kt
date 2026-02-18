package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import kotlin.math.floor

data class CellCoord(
    val x: Long,
    val y: Long,
)

data class GeoPoint(
    val longitude: Double,
    val latitude: Double,
    val weight: Int = 1,
)

data class ClusterPhotoMember(
    val id: Long,
    val cell: CellCoord,
    val point: GeoPoint,
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

    fun resolveClusterPhotoIds(
        zoom: Double,
        photos: List<ClusterPhotoMember>,
        targetClusterId: String,
    ): Set<Long> {
        val parsed = ClusterId.parse(targetClusterId)
        val targetCell = CellCoord(parsed.cellX, parsed.cellY)
        val photosByCell = photos.groupBy({ it.cell }, { it.point })
        val cells =
            resolveClusterCells(zoom = floor(zoom).toInt(), photosByCell = photosByCell, targetCell = targetCell)
        val memberCells = if (cells.isEmpty()) setOf(targetCell) else cells
        return photos.filter { it.cell in memberCells }.map { it.id }.toSet()
    }
}
