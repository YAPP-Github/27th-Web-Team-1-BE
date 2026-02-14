package kr.co.lokit.api.domain.photo.scheduler

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.domain.photo.infrastructure.PhotoJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = false)
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

        val (dbUrlsFuture, s3KeysFuture) =
            StructuredConcurrency.run { scope ->
                Pair(
                    scope.fork { photoJpaRepository.findUrlsCreatedSince(since).toSet() },
                    scope.fork { listRecentS3Keys(cutoff) },
                )
            }

        val dbUrls = dbUrlsFuture.get()
        val recentS3Keys = s3KeysFuture.get()

        val orphanKeys =
            recentS3Keys.filter { key ->
                val objectUrl = OBJECT_URL_TEMPLATE.format(bucket, region, key)
                objectUrl !in dbUrls
            }

        orphanKeys.chunked(MAX_BATCH_DELETE_SIZE).forEach { chunk ->
            retry(actionName = "orphan-delete", context = "chunkSize=${chunk.size}") {
                val delete =
                    Delete
                        .builder()
                        .objects(chunk.map { key -> ObjectIdentifier.builder().key(key).build() })
                        .quiet(true)
                        .build()
                s3Client.deleteObjects(
                    DeleteObjectsRequest
                        .builder()
                        .bucket(bucket)
                        .delete(delete)
                        .build(),
                )
            }
        }

        if (orphanKeys.isNotEmpty()) {
            log.info("고아 객체 {}개 삭제 완료", orphanKeys.size)
        }
    }

    private fun listRecentS3Keys(cutoff: Instant): List<String> {
        val keys = mutableListOf<String>()
        var continuationToken: String? = null

        do {
            val request =
                ListObjectsV2Request
                    .builder()
                    .bucket(bucket)
                    .prefix(PREFIX)
                    .apply { continuationToken?.let { continuationToken(it) } }
                    .build()

            val response =
                retry(actionName = "orphan-list-objects", context = "prefix=$PREFIX") {
                    s3Client.listObjectsV2(request)
                }
            response
                .contents()
                .filter { it.lastModified().isAfter(cutoff) }
                .forEach { keys.add(it.key()) }
            continuationToken = if (response.isTruncated) response.nextContinuationToken() else null
        } while (continuationToken != null)

        return keys
    }

    private fun <T> retry(
        actionName: String,
        context: String,
        maxAttempts: Int = 3,
        initialDelayMs: Long = 200L,
        block: () -> T,
    ): T {
        var delayMs = initialDelayMs
        var last: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                last = e
                if (attempt == maxAttempts - 1) throw e
                log.warn("{} 재시도: attempt={}/{}, context={}", actionName, attempt + 1, maxAttempts, context, e)
                Thread.sleep(delayMs)
                delayMs *= 2
            }
        }
        throw last ?: IllegalStateException("retry failed: $actionName")
    }

    companion object {
        private const val PREFIX = "photos/"
        private const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/%s"
        private const val MAX_BATCH_DELETE_SIZE = 1000
    }
}
