package kr.co.lokit.api.domain.album.domain

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.photo.domain.Photo

data class Album(
    val title: String,
) {
    var id: Long? = null
    var photos: List<Photo> = emptyList()
    var photoCount: Int? = null
    var thumbnail: Photo? = null

    init {
        if (title.length > 10) {
            throw BusinessException.InvalidInputException("앨범 제목은 10자 이내여야 합니다.")
        }
    }
}
