package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.evictKey
import kr.co.lokit.api.domain.album.application.CurrentCoupleAlbumResolver
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
import kr.co.lokit.api.domain.photo.domain.PresignedUpload
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
    private val currentCoupleAlbumResolver: CurrentCoupleAlbumResolver,
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
    ): PresignedUpload {
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
                        if (!photo.albumId.isPositiveId()) {
                            currentCoupleAlbumResolver.requireDefaultAlbum(
                                userId = photo.uploadedById,
                                errorField = ErrorField.UPLOADED_BY_ID,
                            )
                        } else {
                            currentCoupleAlbumResolver.validateAlbumBelongsToCurrentCouple(
                                userId = photo.uploadedById,
                                albumId = photo.albumId!!,
                                errorField = ErrorField.UPLOADED_BY_ID,
                            )
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
        val effectiveAlbumId =
            if (!albumId.isPositiveId()) {
                currentCoupleAlbumResolver.requireDefaultAlbum(
                    userId = userId,
                    errorField = ErrorField.UPLOADED_BY_ID,
                ).id
            } else {
                currentCoupleAlbumResolver.validateAlbumBelongsToCurrentCouple(
                    userId = userId,
                    albumId = albumId!!,
                    errorField = ErrorField.UPLOADED_BY_ID,
                )
                albumId
            }
        val updated = photo.update(effectiveAlbumId, description, longitude, latitude)
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
        if (!photo.canPublishLocationEvent(coupleId)) {
            return
        }
        val targetCoupleId = coupleId ?: return
        eventPublisher.publishEvent(
            PhotoCreatedEvent(
                albumId = photo.albumId!!,
                coupleId = targetCoupleId,
                longitude = photo.location.longitude,
                latitude = photo.location.latitude,
            ),
        )
    }

    private fun publishPhotoLocationUpdatedEventIfNeeded(
        photo: Photo,
        coupleId: Long?,
    ) {
        if (!photo.canPublishLocationEvent(coupleId)) {
            return
        }
        val targetCoupleId = coupleId ?: return
        eventPublisher.publishEvent(
            PhotoLocationUpdatedEvent(
                albumId = photo.albumId!!,
                coupleId = targetCoupleId,
                longitude = photo.location.longitude,
                latitude = photo.location.latitude,
            ),
        )
    }

    private fun evictMapCachesForPhotoIfNeeded(photo: Photo) {
        if (!photo.canEvictMapCache()) {
            return
        }
        val coupleId = photo.coupleId!!
        cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, coupleId)
        mapPhotosCacheService.evictForPhotoMutation(
            coupleId = coupleId,
            albumId = photo.albumId,
            longitude = photo.location.longitude,
            latitude = photo.location.latitude,
        )
    }

    private fun Long?.isPositiveId(): Boolean = this != null && this > 0L
}
