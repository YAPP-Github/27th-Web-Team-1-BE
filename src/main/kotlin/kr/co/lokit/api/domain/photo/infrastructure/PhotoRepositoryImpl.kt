package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class PhotoRepositoryImpl(
    private val photoJpaRepository: PhotoJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
) : PhotoRepository {

    override fun save(photo: Photo): Photo {
        val albumEntity = albumJpaRepository.findByIdOrNull(photo.albumId)
            ?: throw entityNotFound<Album>(photo.albumId)
        val photoEntity = photo.toEntity(albumEntity)
        val savedEntity = photoJpaRepository.save(photoEntity)
        return savedEntity.toDomain()
    }
}
