package kr.co.lokit.api.domain.photo.application.port.`in`

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse

interface GetPhotoDetailUseCase {
    fun getPhotosByAlbum(
        albumId: Long,
        userId: Long,
    ): List<Album>

    fun getPhotoDetail(
        photoId: Long,
        userId: Long,
    ): PhotoDetailResponse
}
