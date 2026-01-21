package kr.co.lokit.api.domain.album.domain

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.photo.domain.Photo

data class Album(
    val id: Long = 0L,
    val title: String,
    val photoCount: Int = 0,
) {
    var photos: List<Photo> = mutableListOf()
    var thumbnail: Photo? = null

    init {
        if (title.length > 10) {
            throw BusinessException.InvalidInputException("앨범 제목은 10자 이내여야 합니다.")
        }
    }
}
