package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.util.DateTimeUtils.toDateString
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.infrastructure.geocoding.MapClient
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.photo.infrastructure.file.S3PresignedUrlGenerator
import kr.co.lokit.api.domain.photo.mapping.toPhotoListResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PhotoService(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val albumBoundsService: AlbumBoundsService,
    private val s3PresignedUrlGenerator: S3PresignedUrlGenerator?,
    private val mapClient: MapClient,
) {
    @Transactional(readOnly = true)
    fun getPhotosByAlbum(): PhotoListResponse = albumRepository.findAllWithPhotos().toPhotoListResponse()

    fun generatePresignedUrl(
        fileName: String,
        contentType: String,
        userId: Long,
    ): PresignedUrl {
        val key = KEY_TEMPLATE.format(fileName, contentType)
        return s3PresignedUrlGenerator?.generate(key, contentType)
            ?: throw UnsupportedOperationException("S3 is not enabled")
    }

    @Transactional
    fun create(photo: Photo): Photo {
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
            ?: throw entityNotFound<Photo>(photoId)

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

    @Transactional
    fun update(photoId: Long, request: UpdatePhotoRequest): Photo {
        val photo = photoRepository.update(photoId, request)
        if (request.longitude != null && request.latitude != null) {
            albumBoundsService.updateBoundsOnPhotoAdd(
                photo.albumId,
                photo.location.longitude,
                photo.location.latitude,
            )
        }
        return photo
    }

    @Transactional
    fun delete(photoId: Long) =
        photoRepository.deleteById(photoId)

    companion object {
        const val KEY_TEMPLATE = "photos/%d/%s_%s"
    }
}
