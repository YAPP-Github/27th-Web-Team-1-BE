package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.exception.BusinessException
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component
class CoupleInviteRateLimiter {
    private val verificationFailures = ConcurrentHashMap<String, FailureCounter>()

    fun checkVerificationAllowed(
        userId: Long,
        clientIp: String,
    ) {
        val key = verificationKey(userId, clientIp)
        val now = Instant.now()
        val blockedUntil = verificationFailures[key]?.blockedUntil
        if (blockedUntil != null && blockedUntil.isAfter(now)) {
            throw BusinessException.InviteTooManyRequestsException()
        }
    }

    fun recordVerificationFailure(
        userId: Long,
        clientIp: String,
    ) {
        val key = verificationKey(userId, clientIp)
        val now = Instant.now()
        verificationFailures.compute(key) { _, current ->
            val state = current ?: FailureCounter(windowStart = now, count = 0, blockedUntil = null)
            val refreshed =
                if (state.windowStart.plus(FAILURE_WINDOW_SECONDS, ChronoUnit.SECONDS).isBefore(now)) {
                    FailureCounter(windowStart = now, count = 1, blockedUntil = null)
                } else {
                    state.copy(count = state.count + 1)
                }

            if (refreshed.count >= MAX_FAILURES) {
                refreshed.copy(blockedUntil = now.plus(COOLDOWN_SECONDS, ChronoUnit.SECONDS))
            } else {
                refreshed
            }
        }
    }

    fun clearVerificationFailures(
        userId: Long,
        clientIp: String,
    ) {
        verificationFailures.remove(verificationKey(userId, clientIp))
    }

    private fun verificationKey(
        userId: Long,
        clientIp: String,
    ): String = "$userId:$clientIp"

    private data class FailureCounter(
        val windowStart: Instant,
        val count: Int,
        val blockedUntil: Instant?,
    )

    companion object {
        private const val FAILURE_WINDOW_SECONDS = 60L
        private const val MAX_FAILURES = 5
        private const val COOLDOWN_SECONDS = 60L
    }
}
