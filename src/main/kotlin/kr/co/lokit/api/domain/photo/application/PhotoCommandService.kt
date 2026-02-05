package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.dto.isValidId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.map.application.MapPhotosCacheService
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.application.port.`in`.CreatePhotoUseCase
import kr.co.lokit.api.domain.photo.application.port.`in`.UpdatePhotoUseCase
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoCreatedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoDeletedEvent
import kr.co.lokit.api.domain.photo.domain.PhotoLocationUpdatedEvent
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PhotoCommandService(
    private val photoRepository: PhotoRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val photoStoragePort: PhotoStoragePort?,
    private val mapQueryService: SearchLocationUseCase,
    private val eventPublisher: ApplicationEventPublisher,
    private val mapPhotosCacheService: MapPhotosCacheService,
) : CreatePhotoUseCase, UpdatePhotoUseCase {

    @Cacheable(cacheNames = ["presignedUrlCache"], key = "#idempotencyKey", unless = "#idempotencyKey == null")
    override fun generatePresignedUrl(
        idempotencyKey: String?,
        contentType: String,
    ): PresignedUrl {
        val uniqueKey = idempotencyKey ?: UUID.randomUUID().toString()
        val key = KEY_TEMPLATE.format(uniqueKey)
        return photoStoragePort?.generatePresignedUrl(key, contentType)
            ?: throw UnsupportedOperationException("S3 is not enabled")
    }

    @OptimisticRetry
    @Transactional
    @CacheEvict(cacheNames = ["userAlbums"], key = "#photo.uploadedById")
    override fun create(photo: Photo): Photo {
        photoStoragePort?.verifyFileExists(photo.url)
        val locationInfo = mapQueryService.getLocationInfo(photo.location.longitude, photo.location.latitude)
        val effectivePhoto =
            if (!isValidId(photo.albumId)) {
                val defaultAlbum =
                    albumRepository.findDefaultByUserId(photo.uploadedById)
                        ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                            errors = mapOf("uploadedById" to photo.uploadedById.toString()),
                        )
                photo.copy(albumId = defaultAlbum.id, address = locationInfo.address)
            } else {
                photo
            }
        val saved = photoRepository.save(effectivePhoto)

        if (effectivePhoto.hasLocation()) {
            eventPublisher.publishEvent(
                PhotoCreatedEvent(
                    albumId = effectivePhoto.albumId!!,
                    userId = saved.uploadedById,
                    longitude = effectivePhoto.location.longitude,
                    latitude = effectivePhoto.location.latitude,
                ),
            )
        }
        mapPhotosCacheService.evictForUser(photo.uploadedById)
        return saved
    }

    @OptimisticRetry
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["photo"], key = "#userId + ':' + #id"),
            CacheEvict(cacheNames = ["userAlbums"], key = "#userId"),
        ],
    )
    override fun update(
        id: Long,
        albumId: Long,
        description: String?,
        longitude: Double,
        latitude: Double,
        userId: Long,
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
                    userId = result.uploadedById,
                    longitude = updated.location.longitude,
                    latitude = updated.location.latitude,
                ),
            )
        }

        mapPhotosCacheService.evictForUser(userId)
        return result
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["photo"], key = "#userId + ':' + #photoId"),
            CacheEvict(cacheNames = ["userAlbums"], key = "#userId"),
        ],
    )
    override fun delete(photoId: Long, userId: Long) {
        val photo = photoRepository.findById(photoId)
        photoRepository.deleteById(photoId)
        mapPhotosCacheService.evictForUser(userId)
        eventPublisher.publishEvent(PhotoDeletedEvent(photoUrl = photo.url))
    }

    companion object {
        const val KEY_TEMPLATE = "photos/%s"
    }
}
