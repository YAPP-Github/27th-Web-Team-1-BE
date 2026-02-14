package kr.co.lokit.api.domain.couple.scheduler

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = false)
class CoupleGracePeriodScheduler(
    private val coupleJpaRepository: CoupleJpaRepository,
    private val couplePurgeWorker: CouplePurgeWorker,
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
    fun purgeExpiredCouples() {
        val cutoff = LocalDateTime.now().minusDays(GracePeriodPolicy.PURGE_TOTAL_DAYS)
        val coupleIds = coupleJpaRepository.findExpiredBefore(cutoff).map { it.nonNullId() }
        if (coupleIds.isEmpty()) {
            return
        }

        StructuredConcurrency.run { scope ->
            coupleIds.chunked(PURGE_PARALLELISM).forEach { batch ->
                scope.fork {
                    batch.forEach { coupleId ->
                        couplePurgeWorker.purgeCoupleSafely(coupleId)
                    }
                }
            }
        }

        log.info("파기 처리 완료: {}건의 커플 데이터 soft delete", coupleIds.size)
    }

    companion object {
        private const val PURGE_PARALLELISM = 4
    }
}
