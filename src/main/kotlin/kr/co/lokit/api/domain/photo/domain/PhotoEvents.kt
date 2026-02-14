package kr.co.lokit.api.domain.photo.domain

data class PhotoCreatedEvent(
    val albumId: Long,
    val coupleId: Long,
    val longitude: Double,
    val latitude: Double,
)

data class PhotoLocationUpdatedEvent(
    val albumId: Long,
    val coupleId: Long,
    val longitude: Double,
    val latitude: Double,
)

data class PhotoDeletedEvent(
    val photoUrl: String,
)
