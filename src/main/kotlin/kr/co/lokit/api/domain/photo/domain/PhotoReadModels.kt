package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDateTime

data class PresignedUpload(
    val presignedUrl: String,
    val objectUrl: String,
)

data class PhotoDetailReadModel(
    val id: Long,
    val url: String,
    val takenAt: LocalDateTime?,
    val albumName: String,
    val uploadedById: Long,
    val uploaderName: String,
    val uploaderProfileImageUrl: String?,
    val address: String?,
    val description: String?,
    val longitude: Double,
    val latitude: Double,
)
