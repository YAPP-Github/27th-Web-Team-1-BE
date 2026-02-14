package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import kr.co.lokit.api.domain.photo.mapping.toPhotoDetail
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaPhotoRepository(
    private val photoJpaRepository: PhotoJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : PhotoRepositoryPort {
    @Transactional
    override fun save(photo: Photo): Photo {
        val albumEntity =
            albumJpaRepository.findByIdOrNull(photo.albumId!!)
                ?: throw entityNotFound<Album>(photo.albumId)
        val userEntity =
            userJpaRepository.findByIdOrNull(photo.uploadedById)
                ?: throw entityNotFound<User>(photo.uploadedById)
        val photoEntity = photo.toEntity(albumEntity, userEntity)
        val savedEntity = photoJpaRepository.save(photoEntity)
        return savedEntity.toDomain()
    }

    override fun findDetailById(id: Long): PhotoDetail =
        photoJpaRepository.findByIdWithRelations(id)?.toPhotoDetail()
            ?: throw entityNotFound<Photo>(id)

    @Transactional
    override fun deleteById(id: Long) {
        val photoEntity =
            photoJpaRepository.findByIdWithRelations(id)
                ?: throw entityNotFound<Photo>(id)

        photoEntity.album.onPhotoRemoved()
        photoJpaRepository.deleteById(id)
    }

    override fun findAllByUserId(userId: Long): List<Photo> =
        photoJpaRepository
            .findAllByUploadedById(userId)
            .map { it.toDomain() }

    override fun findById(id: Long): Photo =
        photoJpaRepository.findByIdWithRelations(id)?.toDomain()
            ?: throw entityNotFound<Photo>(id)

    override fun saveAll(photos: List<Photo>): List<Photo> {
        if (photos.isEmpty()) return emptyList()
        val first = photos.first()
        val albumEntity =
            albumJpaRepository.findByIdOrNull(first.albumId!!)
                ?: throw entityNotFound<Album>(first.albumId)
        val userEntity =
            userJpaRepository.findByIdOrNull(first.uploadedById)
                ?: throw entityNotFound<User>(first.uploadedById)
        val entities = photos.map { it.toEntity(albumEntity, userEntity) }
        return photoJpaRepository.saveAll(entities).map { it.toDomain() }
    }

    @Transactional
    override fun update(photo: Photo): Photo {
        val albumEntity =
            albumJpaRepository.findByIdOrNull(photo.albumId!!)
                ?: throw entityNotFound<Album>(photo.albumId)
        val userEntity =
            userJpaRepository.findByIdOrNull(photo.uploadedById)
                ?: throw entityNotFound<User>(photo.uploadedById)
        val photoEntity =
            photoJpaRepository.findByIdWithRelations(photo.id)
                ?: throw entityNotFound<Photo>(photo.id)

        val oldAlbum = photoEntity.album
        if (oldAlbum.nonNullId() != albumEntity.nonNullId()) {
            oldAlbum.onPhotoRemoved()
            albumEntity.onPhotoAdded()
        }

        photoEntity.apply(photo = photo, album = albumEntity, uploadedBy = userEntity)
        return photoEntity.toDomain()
    }
}
