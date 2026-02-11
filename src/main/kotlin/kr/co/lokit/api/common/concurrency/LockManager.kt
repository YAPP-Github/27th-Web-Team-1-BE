package kr.co.lokit.api.common.concurrency

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@Component
class LockManager {
    companion object {
        private val locks = ConcurrentHashMap<String, ReentrantLock>()
        const val DEFAULT_TIMEOUT_SECONDS = 10L
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> withLock(
        key: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        operation: () -> T,
    ): T {
        val lock = getOrCreateLock(key)
        val acquired = lock.tryLock(timeoutSeconds, TimeUnit.SECONDS)

        if (!acquired) {
            throw RuntimeException("Email lock timeout after ${timeoutSeconds}s: $key")
        }

        return try {
            operation()
        } finally {
            unlockSafely(key, lock)
        }
    }

    private fun getOrCreateLock(email: String): ReentrantLock = locks.computeIfAbsent(email) { ReentrantLock(true) }

    private fun unlockSafely(
        email: String,
        lock: ReentrantLock,
    ) {
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
        if (!lock.hasQueuedThreads()) {
            locks.remove(email, lock)
        }
    }
}
