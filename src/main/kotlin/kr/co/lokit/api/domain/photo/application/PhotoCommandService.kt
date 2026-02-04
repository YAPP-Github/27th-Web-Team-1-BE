package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.application.port.`in`.CreatePhotoUseCase
import kr.co.lokit.api.domain.photo.application.port.`in`.UpdatePhotoUseCase
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoCreatedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoLocationUpdatedEvent
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import org.springframework.context.ApplicationEventPublisher
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PhotoCommandService(
    private val photoRepository: PhotoRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val photoStoragePort: PhotoStoragePort?,
    private val eventPublisher: ApplicationEventPublisher,
) : CreatePhotoUseCase, UpdatePhotoUseCase {

    override fun generatePresignedUrl(
        fileName: String,
        contentType: String,
    ): PresignedUrl {
        val key = KEY_TEMPLATE.format(UUID.randomUUID(), fileName)
        return photoStoragePort?.generatePresignedUrl(key, contentType)
            ?: throw UnsupportedOperationException("S3 is not enabled")
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    override fun create(photo: Photo): Photo {
        photoStoragePort?.verifyFileExists(photo.url)
        val effectivePhoto =
            if (photo.albumId == null) {
                val defaultAlbum =
                    albumRepository.findDefaultByUserId(photo.uploadedById)
                        ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                            errors = mapOf("uploadedById" to photo.uploadedById.toString()),
                        )
                photo.copy(albumId = defaultAlbum.id)
            } else {
                photo
            }
        val saved = photoRepository.save(effectivePhoto)

        if (effectivePhoto.hasLocation()) {
            eventPublisher.publishEvent(
                PhotoCreatedEvent(
                    albumId = effectivePhoto.albumId!!,
                    longitude = effectivePhoto.location.longitude,
                    latitude = effectivePhoto.location.latitude,
                ),
            )
        }

        return saved
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    override fun update(
        id: Long,
        albumId: Long,
        description: String?,
        longitude: Double,
        latitude: Double,
    ): Photo {
        val photo = photoRepository.findById(id)
        val updated = photo.copy(
            albumId = albumId,
            description = description,
            location = photo.location.copy(
                longitude = longitude,
                latitude = latitude,
            ),
        )
        val result = photoRepository.apply(updated)

        if (updated.hasLocation()) {
            eventPublisher.publishEvent(
                PhotoLocationUpdatedEvent(
                    albumId = updated.albumId!!,
                    longitude = updated.location.longitude,
                    latitude = updated.location.latitude,
                ),
            )
        }

        return result
    }

    @Transactional
    override fun delete(photoId: Long) {
        photoRepository.deleteById(photoId)
    }

    companion object {
        const val KEY_TEMPLATE = "photos/%s/%s"
    }
}
