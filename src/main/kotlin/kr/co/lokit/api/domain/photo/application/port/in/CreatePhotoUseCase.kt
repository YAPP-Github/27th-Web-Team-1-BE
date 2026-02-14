package kr.co.lokit.api.domain.photo.application.port.`in`

import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.PresignedUrl

interface CreatePhotoUseCase {
    fun create(photo: Photo): Photo

    fun generatePresignedUrl(
        idempotencyKey: String?,
        contentType: String,
    ): PresignedUrl
}
