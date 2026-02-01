package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.dto.PageResult
import java.time.LocalDateTime

data class ClusterProjection(
    val cellX: Long,
    val cellY: Long,
    val count: Int,
    val thumbnailUrl: String,
    val centerLongitude: Double,
    val centerLatitude: Double,
)

data class PhotoProjection(
    val id: Long,
    val url: String,
    val longitude: Double,
    val latitude: Double,
    val takenAt: LocalDateTime,
)

data class ClusterPhotoProjection(
    val id: Long,
    val url: String,
    val longitude: Double,
    val latitude: Double,
    val takenAt: LocalDateTime,
)

data class ClusterCandidate(
    val cellX: Long,
    val cellY: Long,
    val count: Int,
    val thumbnailUrl: String,
    val centerLongitude: Double,
    val centerLatitude: Double,
    val rank: Int,
)

interface MapRepository {
    fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        albumId: Long? = null,
    ): List<ClusterProjection>

    fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        albumId: Long? = null,
    ): List<PhotoProjection>

    fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        page: Int,
        size: Int,
    ): PageResult<ClusterPhotoProjection>
}
