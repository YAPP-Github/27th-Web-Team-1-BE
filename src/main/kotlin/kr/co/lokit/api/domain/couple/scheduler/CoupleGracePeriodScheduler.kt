package kr.co.lokit.api.domain.couple.scheduler

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.photo.infrastructure.PhotoJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import java.time.LocalDateTime
import jakarta.persistence.EntityManager

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = false)
class CoupleGracePeriodScheduler(
    private val coupleJpaRepository: CoupleJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val photoJpaRepository: PhotoJpaRepository,
    private val entityManager: EntityManager,
    private val s3Client: S3Client,
    @Value("\${aws.s3.bucket}") private val bucket: String,
    @Value("\${aws.s3.region}") private val region: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    fun expireDisconnectedCouples() {
        val cutoff = LocalDateTime.now().minusDays(GracePeriodPolicy.RECONNECT_DAYS)
        val couples = coupleJpaRepository.findDisconnectedBefore(cutoff)

        couples.forEach { couple ->
            couple.status = CoupleStatus.EXPIRED
        }

        if (couples.isNotEmpty()) {
            log.info("유예기간 만료 처리: {}건의 커플 상태를 EXPIRED로 변경", couples.size)
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    fun purgeExpiredCouples() {
        val cutoff = LocalDateTime.now().minusDays(GracePeriodPolicy.PURGE_TOTAL_DAYS)
        val couples = coupleJpaRepository.findExpiredBefore(cutoff)

        couples.forEach { couple ->
            val coupleId = couple.nonNullId()
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

        if (couples.isNotEmpty()) {
            log.info("파기 처리 완료: {}건의 커플 데이터 soft delete", couples.size)
        }
    }

    private fun softDeleteEmoticons(albumIds: List<Long>) {
        entityManager.createNativeQuery(
            """
            UPDATE emoticon SET is_deleted = true
            WHERE comment_id IN (
                SELECT c.id FROM comment c
                WHERE c.photo_id IN (
                    SELECT p.id FROM photo p WHERE p.album_id IN (:albumIds)
                )
            ) AND is_deleted = false
            """.trimIndent()
        ).setParameter("albumIds", albumIds).executeUpdate()
    }

    private fun softDeleteComments(albumIds: List<Long>) {
        entityManager.createNativeQuery(
            """
            UPDATE comment SET is_deleted = true
            WHERE photo_id IN (
                SELECT p.id FROM photo p WHERE p.album_id IN (:albumIds)
            ) AND is_deleted = false
            """.trimIndent()
        ).setParameter("albumIds", albumIds).executeUpdate()
    }

    private fun softDeletePhotos(albumIds: List<Long>) {
        entityManager.createNativeQuery(
            """
            UPDATE photo SET is_deleted = true
            WHERE album_id IN (:albumIds) AND is_deleted = false
            """.trimIndent()
        ).setParameter("albumIds", albumIds).executeUpdate()
    }

    private fun softDeleteAlbums(albumIds: List<Long>) {
        entityManager.createNativeQuery(
            """
            UPDATE album SET is_deleted = true
            WHERE id IN (:albumIds) AND is_deleted = false
            """.trimIndent()
        ).setParameter("albumIds", albumIds).executeUpdate()
    }

    private fun softDeleteCoupleUsers(coupleId: Long) {
        entityManager.createNativeQuery(
            """
            UPDATE couple_user SET is_deleted = true
            WHERE couple_id = :coupleId AND is_deleted = false
            """.trimIndent()
        ).setParameter("coupleId", coupleId).executeUpdate()
    }

    private fun softDeleteCouple(coupleId: Long) {
        entityManager.createNativeQuery(
            """
            UPDATE couple SET is_deleted = true
            WHERE id = :coupleId AND is_deleted = false
            """.trimIndent()
        ).setParameter("coupleId", coupleId).executeUpdate()
    }

    private fun deleteS3Files(urls: List<String>) {
        if (urls.isEmpty()) return

        val prefix = OBJECT_URL_TEMPLATE.format(bucket, region)
        val keys = urls.mapNotNull { url ->
            if (url.startsWith(prefix)) url.removePrefix(prefix) else null
        }

        keys.chunked(MAX_BATCH_DELETE_SIZE).forEach { chunk ->
            val delete = Delete.builder()
                .objects(chunk.map { key -> ObjectIdentifier.builder().key(key).build() })
                .quiet(true)
                .build()
            s3Client.deleteObjects(
                DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(delete)
                    .build()
            )
        }
    }

    companion object {
        private const val OBJECT_URL_TEMPLATE = "https://%s.s3.%s.amazonaws.com/"
        private const val MAX_BATCH_DELETE_SIZE = 1000
    }
}
