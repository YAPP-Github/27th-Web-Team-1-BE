package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDateTime

data class PhotoDetail(
    val id: Long,
    val url: String,
    val takenAt: LocalDateTime?,
    val albumName: String,
    val uploadedById: Long,
    val uploaderName: String,
    val uploaderProfileImageUrl: String?,
    val location: Location,
    val description: String?,
)
