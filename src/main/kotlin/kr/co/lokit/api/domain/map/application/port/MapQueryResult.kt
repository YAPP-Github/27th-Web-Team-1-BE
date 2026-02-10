package kr.co.lokit.api.domain.map.application.port

import java.time.LocalDateTime

data class ClusterProjection(
    val cellX: Long,
    val cellY: Long,
    val count: Int,
    val thumbnailUrl: String,
    val centerLongitude: Double,
    val centerLatitude: Double,
    val takenAt: LocalDateTime? = null,
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
    val address: String
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

data class UniquePhotoRecord(
    val id: Long,
    val url: String,
    val longitude: Double,
    val latitude: Double,
    val cellX: Long,
    val cellY: Long,
    val takenAt: LocalDateTime,
    val count: Int = 1,
    val avgLongitude: Double? = null,
    val avgLatitude: Double? = null,
)

data class GridKey(val cellX: Long, val cellY: Long)

data class ClusterData(
    val gridKey: GridKey,
    val count: Int,
    val centerLongitude: Double,
    val centerLatitude: Double,
    val photosByRank: List<RankedPhoto>,
    val takenAt: LocalDateTime? = null,
)

data class RankedPhoto(
    val url: String,
    val rank: Int,
)
