package kr.co.lokit.api.album.domain

import kr.co.lokit.api.photo.domain.Photo

data class Album(
    val id: Long,
    val title: String,
    val photos: List<Photo>,
    val photoCount: Int = 0,
) {
}
