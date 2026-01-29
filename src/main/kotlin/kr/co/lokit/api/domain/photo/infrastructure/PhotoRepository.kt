package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest

interface PhotoRepository {
    fun save(photo: Photo): Photo

    fun findDetailById(id: Long): PhotoDetail?

    fun update(id: Long, request: UpdatePhotoRequest): Photo

    fun deleteById(id: Long)
}
