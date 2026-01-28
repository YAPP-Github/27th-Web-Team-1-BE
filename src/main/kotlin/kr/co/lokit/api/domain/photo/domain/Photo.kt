package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDateTime

data class Photo(
    val id: Long = 0L,
    val url: String,
    val albumId: Long,
    val uploadedById: Long,
    val location: Location,
    val description: String? = null,
    val takenAt: LocalDateTime,
)
