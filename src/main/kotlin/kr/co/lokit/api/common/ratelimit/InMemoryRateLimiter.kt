package kr.co.lokit.api.common.ratelimit

import kr.co.lokit.api.common.exception.BusinessException
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryRateLimiter {
    private val counters = ConcurrentHashMap<String, WindowCounter>()

    fun checkAllowed(
        key: String,
        windowSeconds: Long,
        maxRequests: Int,
    ) {
        val now = Instant.now()
        val exceeded =
            counters.compute(key) { _, current ->
                val state = current ?: WindowCounter(windowStart = now, count = 0)
                if (state.windowStart.plus(windowSeconds, ChronoUnit.SECONDS).isBefore(now)) {
                    WindowCounter(windowStart = now, count = 1)
                } else {
                    state.copy(count = state.count + 1)
                }
            }!!.count > maxRequests

        if (exceeded) {
            throw BusinessException.InviteTooManyRequestsException()
        }
    }

    private data class WindowCounter(
        val windowStart: Instant,
        val count: Int,
    )
}
