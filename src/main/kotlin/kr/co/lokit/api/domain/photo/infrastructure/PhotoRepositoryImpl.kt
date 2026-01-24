package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.BusinessException.AlbumNotFoundException
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import org.springframework.stereotype.Repository

@Repository
class PhotoRepositoryImpl(
    private val photoJpaRepository: PhotoJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
) : PhotoRepository {

    override fun save(photo: Photo): Photo {
        val albumEntity = albumJpaRepository.findById(photo.albumId)
            .orElseThrow { AlbumNotFoundException() }
        val photoEntity = photo.toEntity(albumEntity)
        val savedEntity = photoJpaRepository.save(photoEntity)
        return savedEntity.toDomain()
    }
}
