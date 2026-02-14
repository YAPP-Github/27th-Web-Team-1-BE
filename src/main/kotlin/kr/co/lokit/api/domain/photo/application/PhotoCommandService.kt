package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.dto.isValidId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.evictKey
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
import kr.co.lokit.api.domain.photo.domain.PhotoStorageKey
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
        cacheNames = [CacheNames.PRESIGNED_URL],
        key = "#idempotencyKey",
        condition = "#idempotencyKey != null",
        sync = true,
    )
    override fun generatePresignedUrl(
        idempotencyKey: String?,
        contentType: String,
    ): PresignedUrl {
        val uniqueKey = idempotencyKey ?: UUID.randomUUID().toString()
        val key = PhotoStorageKey.fromUniqueToken(uniqueKey)
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
                                    errors = errorDetailsOf(ErrorField.UPLOADED_BY_ID to photo.uploadedById),
                                )
                        } else {
                            null
                        }
                    },
                )
            }

        val locationInfo = locationFuture.get()
        val defaultAlbum = defaultAlbumFuture.get()
        val effectivePhoto = photo.withAddress(locationInfo.address).withDefaultAlbum(defaultAlbum?.id)
        val saved = photoRepository.save(effectivePhoto)

        publishPhotoCreatedEventIfNeeded(effectivePhoto, saved.coupleId)
        evictMapCachesForPhotoIfNeeded(saved)
        return saved
    }

    @OptimisticRetry
    @Transactional
    @CacheEvict(cacheNames = [CacheNames.PHOTO], key = "#userId + ':' + #id")
    override fun update(
        id: Long,
        albumId: Long?,
        description: String?,
        longitude: Double?,
        latitude: Double?,
        userId: Long,
    ): Photo {
        val photo = photoRepository.findById(id)
        val updated = photo.update(albumId, description, longitude, latitude)
        val result = photoRepository.update(updated)

        publishPhotoLocationUpdatedEventIfNeeded(updated, result.coupleId)
        result.coupleId?.let { cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, it) }
        evictMapCachesForPhotoIfNeeded(photo)
        evictMapCachesForPhotoIfNeeded(result)
        return result
    }

    @Transactional
    @CacheEvict(cacheNames = [CacheNames.PHOTO], key = "#userId + ':' + #photoId")
    override fun delete(
        photoId: Long,
        userId: Long,
    ) {
        val photo = photoRepository.findById(photoId)
        photoRepository.deleteById(photoId)
        evictMapCachesForPhotoIfNeeded(photo)
        eventPublisher.publishEvent(PhotoDeletedEvent(photoUrl = photo.url))
    }

    private fun publishPhotoCreatedEventIfNeeded(
        photo: Photo,
        coupleId: Long?,
    ) {
        if (!photo.hasLocation() || coupleId == null) {
            return
        }
        eventPublisher.publishEvent(
            PhotoCreatedEvent(
                albumId = photo.albumId!!,
                coupleId = coupleId,
                longitude = photo.location.longitude,
                latitude = photo.location.latitude,
            ),
        )
    }

    private fun publishPhotoLocationUpdatedEventIfNeeded(
        photo: Photo,
        coupleId: Long?,
    ) {
        if (!photo.hasLocation() || coupleId == null) {
            return
        }
        eventPublisher.publishEvent(
            PhotoLocationUpdatedEvent(
                albumId = photo.albumId!!,
                coupleId = coupleId,
                longitude = photo.location.longitude,
                latitude = photo.location.latitude,
            ),
        )
    }

    private fun evictMapCachesForPhotoIfNeeded(photo: Photo) {
        val coupleId = photo.coupleId
        if (coupleId == null || !photo.hasLocation()) {
            return
        }
        cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, coupleId)
        mapPhotosCacheService.evictForPhotoMutation(
            coupleId = coupleId,
            albumId = photo.albumId,
            longitude = photo.location.longitude,
            latitude = photo.location.latitude,
        )
    }
}
