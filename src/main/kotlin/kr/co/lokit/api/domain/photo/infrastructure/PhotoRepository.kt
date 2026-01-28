package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail

interface PhotoRepository {
    fun save(photo: Photo): Photo

    fun findDetailById(id: Long): PhotoDetail?
}
