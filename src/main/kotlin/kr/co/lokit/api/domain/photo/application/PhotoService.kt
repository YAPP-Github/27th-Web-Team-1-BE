package kr.co.lokit.api.domain.photo.application

import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.photo.infrastructure.S3PresignedUrlGenerator
import org.springframework.stereotype.Service

@Service
class PhotoService(
    private val photoRepository: PhotoRepository,
    private val s3PresignedUrlGenerator: S3PresignedUrlGenerator?,
) {
    fun generatePresignedUrl(fileName: String, contentType: String, userId: Long): PresignedUrl {
        val key = KEY_TEMPLATE.format(fileName, contentType)
        return s3PresignedUrlGenerator?.generate(key, contentType)
            ?: throw UnsupportedOperationException("S3 is not enabled")
    }

    fun create(photo: Photo): Photo {
        return photoRepository.save(photo)
    }

    companion object {
        const val KEY_TEMPLATE = "photos/%d/%s_%s"
    }
}
