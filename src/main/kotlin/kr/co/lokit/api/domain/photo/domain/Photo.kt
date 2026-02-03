package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDateTime

data class Photo(
    val id: Long = 0L,
    val albumId: Long?,
    val location: Location,
    val description: String? = null,
    var url: String,
    var uploadedById: Long,
    var takenAt: LocalDateTime = LocalDateTime.now(),
) {
    fun hasLocation(): Boolean {
        return location.longitude != 0.0 && location.latitude != 0.0

    }
}
