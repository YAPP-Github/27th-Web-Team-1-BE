package kr.co.lokit.api.domain.photo.infrastructure.file

import kr.co.lokit.api.common.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class S3FileVerifier(
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
) {
    fun verifyNotExists(key: String) {
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            )
            throw BusinessException.InvalidInputException("S3에 이미 파일이 존재합니다: $key")
        } catch (_: NoSuchKeyException) {
            // 존재하지 않으면 정상
        }
    }

    fun verify(objectUrl: String) {
        val key = extractKey(objectUrl)
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            )
        } catch (e: NoSuchKeyException) {
            throw BusinessException.InvalidInputException("S3에 파일이 업로드되지 않았습니다: $key")
        }
    }

    internal fun extractKey(objectUrl: String): String {
        val prefix = OBJECT_URL_TEMPLATE.format(bucket, region)
        require(objectUrl.startsWith(prefix)) { "올바르지 않은 S3 URL입니다: $objectUrl" }
        return objectUrl.removePrefix(prefix)
    }

    companion object {
        const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/"
    }
}
