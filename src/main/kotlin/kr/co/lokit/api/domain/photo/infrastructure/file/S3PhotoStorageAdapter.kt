package kr.co.lokit.api.domain.photo.infrastructure.file

import kr.co.lokit.api.domain.photo.application.port.PhotoStoragePort
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class S3PhotoStorageAdapter(
    private val s3PresignedUrlGenerator: S3PresignedUrlGenerator,
    private val s3FileVerifier: S3FileVerifier,
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String,
) : PhotoStoragePort {
    override fun generatePresignedUrl(
        key: String,
        contentType: String,
    ): PresignedUrl = s3PresignedUrlGenerator.generate(key, contentType)

    override fun verifyFileExists(objectUrl: String) {
        s3FileVerifier.verify(objectUrl)
    }

    override fun verifyFileNotExists(key: String) {
        s3FileVerifier.verifyNotExists(key)
    }

    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 2.0, random = true),
    )
    override fun deleteFileByKey(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build(),
        )
    }

    override fun deleteFileByUrl(url: String) {
        val key = s3FileVerifier.extractKey(url)
        deleteFileByKey(key)
    }
}
