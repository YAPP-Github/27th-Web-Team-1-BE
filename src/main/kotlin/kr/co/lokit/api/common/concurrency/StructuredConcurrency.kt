package kr.co.lokit.api.common.concurrency

import java.util.concurrent.StructuredTaskScope

object StructuredConcurrency {
    fun <R> run(
        block: (scope: StructuredTaskScope.ShutdownOnFailure) -> R,
    ): R =
        StructuredTaskScope.ShutdownOnFailure().use { scope ->
            val result = block(scope)
            scope.join().throwIfFailed()
            result
        }
}

