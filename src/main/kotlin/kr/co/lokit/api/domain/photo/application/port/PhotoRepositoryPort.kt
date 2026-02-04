package kr.co.lokit.api.domain.photo.application.port

import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail

interface PhotoRepositoryPort {
    fun save(photo: Photo): Photo

    fun findDetailById(id: Long): PhotoDetail

    fun deleteById(id: Long)

    fun findAllByUserId(userId: Long): List<Photo>

    fun findById(id: Long): Photo

    fun apply(photo: Photo): Photo

    fun saveAll(photos: List<Photo>): List<Photo>
}

