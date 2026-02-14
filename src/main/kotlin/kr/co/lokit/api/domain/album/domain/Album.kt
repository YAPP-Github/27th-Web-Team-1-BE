package kr.co.lokit.api.domain.album.domain

import kr.co.lokit.api.domain.photo.domain.Photo

data class Album(
    val id: Long = 0L,
    val title: String,
    val coupleId: Long = 0L,
    val createdById: Long = 0L,
    val photoCount: Int = 0,
    val isDefault: Boolean = false,
) {
    var photos: List<Photo> = mutableListOf()

    val thumbnail: Photo?
        get() = photos.maxByOrNull { it.takenAt }

    val thumbnails: List<Photo>
        get() = photos.sortedByDescending { it.takenAt }.take(4)

    init {
        require(title.length <= 10) { "앨범 제목은 10자 이내여야 합니다." }
    }
}
