package kr.co.lokit.api.domain.photo.dto

data class CreatePhotoRequest(
    val url: String,
    val albumId: Long,
    val longitude: Double,
    val latitude: Double,
    val description: String? = null,
)
