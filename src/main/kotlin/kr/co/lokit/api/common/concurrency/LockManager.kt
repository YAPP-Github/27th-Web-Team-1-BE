package kr.co.lokit.api.common.concurrency

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> withLock(
        key: String,
        timeoutSeconds: Long = LockPolicy.JVM_LOCK_TIMEOUT_SECONDS,
        operation: () -> T,
    ): T {
        val lock = getOrCreateLock(key)
        val acquired =
            runCatching { lock.tryLock(timeoutSeconds, TimeUnit.SECONDS) }
                .getOrElse { throwable ->
                    if (throwable is InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    throw BusinessException.LockTimeoutException(
                        errors = errorDetailsOf(ErrorField.LOCK_KEY to key),
                        cause = throwable,
                    )
                }

        if (!acquired) {
            throw BusinessException.LockTimeoutException(
                errors = errorDetailsOf(ErrorField.LOCK_KEY to key),
            )
        }

        return try {
            operation()
        } finally {
            unlockSafely(key, lock)
        }
    }

    private fun getOrCreateLock(key: String): ReentrantLock = locks.computeIfAbsent(key) { ReentrantLock(true) }

    private fun unlockSafely(
        key: String,
        lock: ReentrantLock,
    ) {
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
        if (!lock.hasQueuedThreads()) {
            locks.remove(key, lock)
        }
    }
}
