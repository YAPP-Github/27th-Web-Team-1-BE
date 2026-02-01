package kr.co.lokit.api.domain.photo.infrastructure.file

import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ServerSideEncryption
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class S3PresignedUrlGenerator(
    private val s3Presigner: S3Presigner,
    private val s3FileVerifier: S3FileVerifier,
    @Value("\${aws.s3.region}") private val region: String,
    @Value("\${aws.s3.bucket}") private val bucket: String
) {
    fun generate(key: String, contentType: String): PresignedUrl {
        s3FileVerifier.verifyNotExists(key)

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .serverSideEncryption(ServerSideEncryption.AES256)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(SIGNATURE_DURATION_MINUTES))
            .putObjectRequest(request)
            .build()

        val presignedUrl = s3Presigner.presignPutObject(presignRequest)
        val objectUrl = OBJECT_URL_TEMPLATE.format(bucket, region, key)

        return PresignedUrl(
            presignedUrl = presignedUrl.url().toString(),
            objectUrl = objectUrl
        )
    }

    companion object {
        const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/%s"
        const val SIGNATURE_DURATION_MINUTES = 30L
    }
}
