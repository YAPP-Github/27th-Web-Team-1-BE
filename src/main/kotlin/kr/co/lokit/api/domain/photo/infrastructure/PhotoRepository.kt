package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.domain.photo.domain.Photo

interface PhotoRepository {
    fun save(photo: Photo): Photo
}
