package kr.co.lokit.api.domain.user.scheduler

import kr.co.lokit.api.common.constants.AccountStatus
import kr.co.lokit.api.common.constants.GracePeriodPolicy
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class WithdrawnUserPurgeScheduler(
    private val userJpaRepository: UserJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 30 5 * * *")
    @Transactional
    fun purgeWithdrawnUsers() {
        val cutoff = LocalDateTime.now().minusDays(GracePeriodPolicy.PURGE_TOTAL_DAYS)
        val targets = userJpaRepository.findByStatusAndWithdrawnAtBefore(AccountStatus.WITHDRAWN, cutoff)
        if (targets.isEmpty()) {
            return
        }

        targets.forEach { user ->
            val userId = user.nonNullId()
            user.email = "purged-$userId-${System.currentTimeMillis()}@lokit.co.kr"
            user.name = "탈퇴한 사용자"
            user.profileImageUrl = null
            user.withdrawnAt = null
        }

        log.info("탈퇴 계정 비가역 파기 처리 완료: {}건", targets.size)
    }
}
