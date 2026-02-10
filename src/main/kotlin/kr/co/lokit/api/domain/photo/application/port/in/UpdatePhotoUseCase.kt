package kr.co.lokit.api.domain.photo.application.port.`in`

import kr.co.lokit.api.domain.photo.domain.Photo

interface UpdatePhotoUseCase {
    fun update(
        id: Long,
        albumId: Long?,
        description: String?,
        longitude: Double?,
        latitude: Double?,
        userId: Long,
    ): Photo

    fun delete(photoId: Long, userId: Long)
}
