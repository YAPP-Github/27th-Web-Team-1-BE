package kr.co.lokit.api.domain.photo.domain

data class Photo(
    val id: Long = 0L,
    val url: String,
    val albumId: Long,
    val location: Location,
    val description: String? = null,
)
