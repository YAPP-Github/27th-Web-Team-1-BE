package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.concurrency.StructuredConcurrency
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
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
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
    private val cacheManager: CacheManager,
) : CreatePhotoUseCase,
    UpdatePhotoUseCase {
    @Cacheable(
        cacheNames = ["presignedUrl"],
        key = "#idempotencyKey",
        condition = "#idempotencyKey != null",
        sync = true,
    )
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
    override fun create(photo: Photo): Photo {
        val (_, locationFuture, defaultAlbumFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { photoStoragePort?.verifyFileExists(photo.url) },
                    scope.fork { mapQueryService.getLocationInfo(photo.location.longitude, photo.location.latitude) },
                    scope.fork {
                        if (!isValidId(photo.albumId)) {
                            albumRepository.findDefaultByUserId(photo.uploadedById)
                                ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                                    errors = mapOf("uploadedById" to photo.uploadedById.toString()),
                                )
                        } else {
                            null
                        }
                    },
                )
            }

        val locationInfo = locationFuture.get()
        val defaultAlbum = defaultAlbumFuture.get()

        val effectivePhoto =
            if (defaultAlbum != null) {
                photo.copy(albumId = defaultAlbum.id, address = locationInfo.address)
            } else {
                photo.copy(address = locationInfo.address)
            }
        val saved = photoRepository.save(effectivePhoto)

        if (effectivePhoto.hasLocation()) {
            eventPublisher.publishEvent(
                PhotoCreatedEvent(
                    albumId = effectivePhoto.albumId!!,
                    coupleId = saved.coupleId!!,
                    longitude = effectivePhoto.location.longitude,
                    latitude = effectivePhoto.location.latitude,
                ),
            )
        }
        val coupleId = saved.coupleId
        if (coupleId != null) {
            cacheManager.getCache("coupleAlbums")?.evict(coupleId)
            mapPhotosCacheService.evictForCouple(coupleId)
        }
        return saved
    }

    @OptimisticRetry
    @Transactional
    @CacheEvict(cacheNames = ["photo"], key = "#userId + ':' + #id")
    override fun update(
        id: Long,
        albumId: Long,
        description: String?,
        longitude: Double,
        latitude: Double,
        userId: Long,
    ): Photo {
        val photo = photoRepository.findById(id)
        val updated =
            photo.copy(
                albumId = albumId,
                description = description,
                location =
                    photo.location.copy(
                        longitude = longitude,
                        latitude = latitude,
                    ),
            )
        val result = photoRepository.apply(updated)

        if (updated.hasLocation()) {
            eventPublisher.publishEvent(
                PhotoLocationUpdatedEvent(
                    albumId = updated.albumId!!,
                    coupleId = result.coupleId!!,
                    longitude = updated.location.longitude,
                    latitude = updated.location.latitude,
                ),
            )
        }
        val coupleId = result.coupleId
        if (coupleId != null) {
            cacheManager.getCache("coupleAlbums")?.evict(coupleId)
            mapPhotosCacheService.evictForCouple(coupleId)
        }
        return result
    }

    @Transactional
    @CacheEvict(cacheNames = ["photo"], key = "#userId + ':' + #photoId")
    override fun delete(
        photoId: Long,
        userId: Long,
    ) {
        val photo = photoRepository.findById(photoId)
        photoRepository.deleteById(photoId)
        val coupleId = photo.coupleId
        if (coupleId != null) {
            cacheManager.getCache("coupleAlbums")?.evict(coupleId)
            mapPhotosCacheService.evictForCouple(coupleId)
        }
        eventPublisher.publishEvent(PhotoDeletedEvent(photoUrl = photo.url))
    }

    companion object {
        const val KEY_TEMPLATE = "photos/%s"
    }
}
