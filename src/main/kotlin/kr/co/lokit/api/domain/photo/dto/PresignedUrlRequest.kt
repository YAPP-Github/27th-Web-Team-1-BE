package kr.co.lokit.api.domain.photo.dto

data class PresignedUrlRequest(
    val fileName: String,
    val contentType: String,
) {
}
