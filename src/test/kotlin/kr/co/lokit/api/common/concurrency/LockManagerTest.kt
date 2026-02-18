package kr.co.lokit.api.common.concurrency

import kr.co.lokit.api.common.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LockManagerTest {
    private val lockManager = LockManager()

    @Test
    fun `withLock executes operation when lock can be acquired`() {
        val result =
            lockManager.withLock(
                key = "lock:happy-path",
                timeoutSeconds = 1,
            ) { "ok" }

        assertEquals("ok", result)
    }

    @Test
    fun `withLock throws lock timeout when lock is already held`() {
        val lockAcquired = CountDownLatch(1)
        val releaseLock = CountDownLatch(1)

        val holder =
            thread(start = true) {
                lockManager.withLock(
                    key = "lock:timeout",
                    timeoutSeconds = 3,
                ) {
                    lockAcquired.countDown()
                    releaseLock.await(3, TimeUnit.SECONDS)
                }
            }

        lockAcquired.await(1, TimeUnit.SECONDS)
        assertThrows(BusinessException.LockTimeoutException::class.java) {
            lockManager.withLock(
                key = "lock:timeout",
                timeoutSeconds = 1,
            ) { "never" }
        }

        releaseLock.countDown()
        holder.join(4000)
    }
}
