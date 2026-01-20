package kr.co.lokit.api.album.domain

data class Album(
    val id: Long,
    val title: String,
    val imageCount: Int = 0,
) {
}
