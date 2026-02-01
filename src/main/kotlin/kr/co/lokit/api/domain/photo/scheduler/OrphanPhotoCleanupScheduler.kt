package kr.co.lokit.api.domain.photo.scheduler

import kr.co.lokit.api.domain.photo.infrastructure.PhotoJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class OrphanPhotoCleanupScheduler(
    private val s3Client: S3Client,
    private val photoJpaRepository: PhotoJpaRepository,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanup() {
        val since = LocalDateTime.now().minusDays(1)
        val cutoff = Instant.now().minus(1, ChronoUnit.DAYS)

        val dbUrls = photoJpaRepository.findUrlsCreatedSince(since).toSet()
        val recentS3Keys = listRecentS3Keys(cutoff)

        val orphanKeys = recentS3Keys.filter { key ->
            val objectUrl = OBJECT_URL_TEMPLATE.format(bucket, region, key)
            objectUrl !in dbUrls
        }

        orphanKeys.forEach { key ->
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            )
        }

        if (orphanKeys.isNotEmpty()) {
            log.info("고아 객체 {}개 삭제 완료", orphanKeys.size)
        }
    }

    private fun listRecentS3Keys(cutoff: Instant): List<String> {
        val keys = mutableListOf<String>()
        var continuationToken: String? = null

        do {
            val request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(PREFIX)
                .apply { continuationToken?.let { continuationToken(it) } }
                .build()

            val response = s3Client.listObjectsV2(request)
            response.contents()
                .filter { it.lastModified().isAfter(cutoff) }
                .forEach { keys.add(it.key()) }
            continuationToken = if (response.isTruncated) response.nextContinuationToken() else null
        } while (continuationToken != null)

        return keys
    }

    companion object {
        private const val PREFIX = "photos/"
        private const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/%s"
    }
}
