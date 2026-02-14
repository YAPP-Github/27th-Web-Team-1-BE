package kr.co.lokit.api.common.concurrency

import java.util.concurrent.Semaphore
import java.util.concurrent.StructuredTaskScope

object StructuredConcurrency {
    fun <R> run(block: (scope: StructuredTaskScope.ShutdownOnFailure) -> R): R =
        StructuredTaskScope.ShutdownOnFailure().use { scope ->
            val result = block(scope)
            try {
                scope.join()
                scope.throwIfFailed()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw RuntimeException("작업 실행 중 인터럽트가 발생했습니다.", e)
            }

            result
        }
}

inline fun <T> Semaphore.withPermit(action: () -> T): T {
    this.acquire()
    try {
        return action()
    } finally {
        this.release()
    }
}
