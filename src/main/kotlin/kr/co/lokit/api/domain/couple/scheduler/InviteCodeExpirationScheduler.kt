package kr.co.lokit.api.domain.couple.scheduler

import kr.co.lokit.api.domain.couple.infrastructure.InviteCodeJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class InviteCodeExpirationScheduler(
    private val inviteCodeJpaRepository: InviteCodeJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    fun expireInviteCodes() {
        val deletedCount = inviteCodeJpaRepository.hardDeleteExpiredUnusedCodes(LocalDateTime.now())
        if (deletedCount > 0) {
            log.info("invite_expired_deleted count={}", deletedCount)
        }
    }
}
