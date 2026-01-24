package kr.co.lokit.api.domain.photo.dto

data class PresignedUrl(
    val presignedUrl: String,
    val objectUrl: String,
)
