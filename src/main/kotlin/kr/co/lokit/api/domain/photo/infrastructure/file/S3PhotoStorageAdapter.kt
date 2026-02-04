package kr.co.lokit.api.domain.photo.infrastructure.file

import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class S3PhotoStorageAdapter(
    private val s3PresignedUrlGenerator: S3PresignedUrlGenerator,
    private val s3FileVerifier: S3FileVerifier,
) : PhotoStoragePort {
    override fun generatePresignedUrl(key: String, contentType: String): PresignedUrl =
        s3PresignedUrlGenerator.generate(key, contentType)

    override fun verifyFileExists(objectUrl: String) {
        s3FileVerifier.verify(objectUrl)
    }

    override fun verifyFileNotExists(key: String) {
        s3FileVerifier.verifyNotExists(key)
    }
}
