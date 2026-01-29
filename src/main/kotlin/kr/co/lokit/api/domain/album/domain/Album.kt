package kr.co.lokit.api.domain.album.domain

import kr.co.lokit.api.domain.photo.domain.Photo

data class Album(
    val id: Long = 0L,
    val title: String,
    val workspaceId: Long,
    val photoCount: Int = 0,
) {
    var photos: List<Photo> = mutableListOf()
    var thumbnail: Photo? = null
    var thumbnails: List<Photo> = mutableListOf()

    init {
        require(title.length <= 10) { "앨범 제목은 10자 이내여야 합니다." }
    }
}
