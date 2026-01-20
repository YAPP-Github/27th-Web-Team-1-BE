package kr.co.lokit.api.domain.album.domain

import kr.co.lokit.api.domain.photo.domain.Photo

data class Album(
    val id: Long,
    val title: String,
    val photos: List<Photo>,
    val photoCount: Int = 0,
) {
}
