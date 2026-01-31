package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.util.DateTimeUtils.toDateString
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.infrastructure.geocoding.MapClient
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.photo.infrastructure.file.S3FileVerifier
import kr.co.lokit.api.domain.photo.infrastructure.file.S3PresignedUrlGenerator
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PhotoService(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val albumBoundsService: AlbumBoundsService,
    private val s3PresignedUrlGenerator: S3PresignedUrlGenerator?,
    private val s3FileVerifier: S3FileVerifier?,
    private val mapClient: MapClient,
) {
    @Transactional(readOnly = true)
    fun getPhotosByAlbum(albumId: Long): List<Album> =
        albumRepository.findByIdWithPhotos(albumId)

    fun generatePresignedUrl(
        fileName: String,
        contentType: String,
    ): PresignedUrl {
        val key = KEY_TEMPLATE.format(UUID.randomUUID(), fileName)
        return s3PresignedUrlGenerator?.generate(key, contentType)
            ?: throw UnsupportedOperationException("S3 is not enabled")
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    fun create(photo: Photo): Photo {
        s3FileVerifier?.verify(photo.url)
        val saved = photoRepository.save(photo)
        albumBoundsService.updateBoundsOnPhotoAdd(
            photo.albumId,
            photo.location.longitude,
            photo.location.latitude,
        )
        return saved
    }

    @Transactional(readOnly = true)
    fun getPhotoDetail(photoId: Long): PhotoDetailResponse {
        val photoDetail = photoRepository.findDetailById(photoId)

        val locationInfo = mapClient.reverseGeocode(
            photoDetail.location.longitude,
            photoDetail.location.latitude,
        )

        return PhotoDetailResponse(
            id = photoDetail.id,
            url = photoDetail.url,
            takenAt = photoDetail.takenAt?.toDateString()!!,
            albumName = photoDetail.albumName,
            uploaderName = photoDetail.uploaderName,
            address = locationInfo.address,
            description = photoDetail.description,
        )
    }

    @Retryable(
        retryFor = [ObjectOptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 50, multiplier = 2.0),
    )
    @Transactional
    fun update(
        id: Long,
        albumId: Long,
        description: String?,
        longitude: Double,
        latitude: Double
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
            albumBoundsService.updateBoundsOnPhotoAdd(
                updated.albumId,
                updated.location.longitude,
                updated.location.latitude,
            )
        }
        return result
    }

    @Transactional
    fun delete(photoId: Long) =
        photoRepository.deleteById(photoId)

    companion object {
        const val KEY_TEMPLATE = "photos/%s/%s"
    }
}
