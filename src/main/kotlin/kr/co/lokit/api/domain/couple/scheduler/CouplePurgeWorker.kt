package kr.co.lokit.api.domain.couple.scheduler

import jakarta.persistence.EntityManager
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.photo.infrastructure.PhotoJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = false)
class CouplePurgeWorker(
    private val albumJpaRepository: AlbumJpaRepository,
    private val photoJpaRepository: PhotoJpaRepository,
    private val entityManager: EntityManager,
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun purgeCoupleSafely(coupleId: Long) {
        runCatching { purgeCouple(coupleId) }
            .onFailure { e -> log.error("커플 파기 실패: coupleId={}", coupleId, e) }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun purgeCouple(coupleId: Long) {
        val albumIds = albumJpaRepository.findAlbumIdsByCoupleId(coupleId)
        val photoUrls = photoJpaRepository.findUrlsByCoupleId(coupleId)

        if (albumIds.isNotEmpty()) {
            softDeleteEmoticons(albumIds)
            softDeleteComments(albumIds)
            softDeletePhotos(albumIds)
            softDeleteAlbums(albumIds)
        }

        softDeleteCoupleUsers(coupleId)
        softDeleteCouple(coupleId)
        deleteS3Files(photoUrls)

        log.info("커플(id={}) 데이터 파기 완료: 앨범 {}개, 사진 {}개", coupleId, albumIds.size, photoUrls.size)
    }

    private fun softDeleteEmoticons(albumIds: List<Long>) {
        entityManager
            .createNativeQuery(
                """
                UPDATE emoticon SET is_deleted = true
                WHERE comment_id IN (
                    SELECT c.id FROM comment c
                    WHERE c.photo_id IN (
                        SELECT p.id FROM photo p WHERE p.album_id IN (:albumIds)
                    )
                ) AND is_deleted = false
                """.trimIndent(),
            ).setParameter("albumIds", albumIds)
            .executeUpdate()
    }

    private fun softDeleteComments(albumIds: List<Long>) {
        entityManager
            .createNativeQuery(
                """
                UPDATE comment SET is_deleted = true
                WHERE photo_id IN (
                    SELECT p.id FROM photo p WHERE p.album_id IN (:albumIds)
                ) AND is_deleted = false
                """.trimIndent(),
            ).setParameter("albumIds", albumIds)
            .executeUpdate()
    }

    private fun softDeletePhotos(albumIds: List<Long>) {
        entityManager
            .createNativeQuery(
                """
                UPDATE photo SET is_deleted = true
                WHERE album_id IN (:albumIds) AND is_deleted = false
                """.trimIndent(),
            ).setParameter("albumIds", albumIds)
            .executeUpdate()
    }

    private fun softDeleteAlbums(albumIds: List<Long>) {
        entityManager
            .createNativeQuery(
                """
                UPDATE album SET is_deleted = true
                WHERE id IN (:albumIds) AND is_deleted = false
                """.trimIndent(),
            ).setParameter("albumIds", albumIds)
            .executeUpdate()
    }

    private fun softDeleteCoupleUsers(coupleId: Long) {
        entityManager
            .createNativeQuery(
                """
                UPDATE couple_user SET is_deleted = true
                WHERE couple_id = :coupleId AND is_deleted = false
                """.trimIndent(),
            ).setParameter("coupleId", coupleId)
            .executeUpdate()
    }

    private fun softDeleteCouple(coupleId: Long) {
        entityManager
            .createNativeQuery(
                """
                UPDATE couple SET is_deleted = true
                WHERE id = :coupleId AND is_deleted = false
                """.trimIndent(),
            ).setParameter("coupleId", coupleId)
            .executeUpdate()
    }

    private fun deleteS3Files(urls: List<String>) {
        if (urls.isEmpty()) return

        val prefix = OBJECT_URL_TEMPLATE.format(bucket, region)
        val keys =
            urls.mapNotNull { url ->
                if (url.startsWith(prefix)) url.removePrefix(prefix) else null
            }

        keys.chunked(MAX_BATCH_DELETE_SIZE).forEach { chunk ->
            retry(
                actionName = "s3-delete-objects",
                context = "chunkSize=${chunk.size}",
            ) {
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
    }

    private fun <T> retry(
        actionName: String,
        context: String,
        maxAttempts: Int = 3,
        initialDelayMs: Long = 200L,
        block: () -> T,
    ): T {
        var last: Throwable? = null
        var delayMs = initialDelayMs
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                last = e
                if (attempt == maxAttempts - 1) {
                    throw e
                }
                log.warn("{} 재시도: attempt={}/{}, context={}", actionName, attempt + 1, maxAttempts, context, e)
                Thread.sleep(delayMs)
                delayMs *= 2
            }
        }
        throw last ?: IllegalStateException("retry failed: $actionName")
    }

    companion object {
        private const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/"
        private const val MAX_BATCH_DELETE_SIZE = 1000
    }
}
