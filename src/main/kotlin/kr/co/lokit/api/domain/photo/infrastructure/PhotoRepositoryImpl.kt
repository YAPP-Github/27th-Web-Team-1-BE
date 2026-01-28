package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import kr.co.lokit.api.domain.photo.mapping.toPhotoDetail
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class PhotoRepositoryImpl(
    private val photoJpaRepository: PhotoJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : PhotoRepository {
    override fun save(photo: Photo): Photo {
        val albumEntity =
            albumJpaRepository.findByIdOrNull(photo.albumId)
                ?: throw entityNotFound<Album>(photo.albumId)
        val userEntity =
            userJpaRepository.findByIdOrNull(photo.uploadedById)
                ?: throw entityNotFound<User>(photo.uploadedById)
        val photoEntity = photo.toEntity(albumEntity, userEntity)
        val savedEntity = photoJpaRepository.save(photoEntity)
        return savedEntity.toDomain()
    }

    override fun findDetailById(id: Long): PhotoDetail? =
        photoJpaRepository.findByIdWithRelations(id).orElse(null)?.toPhotoDetail()
}
