package kr.co.lokit.api.common.util

import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.StructuredTaskScope

object ParallelQuery {

    fun <A, B> execute(
        queryA: () -> A,
        queryB: () -> B,
    ): Pair<A, B> {
        val resources = captureTransactionResources()
        StructuredTaskScope.ShutdownOnFailure().use { scope ->
            val taskA = scope.fork { withTransactionResources(resources) { queryA() } }
            val taskB = scope.fork { withTransactionResources(resources) { queryB() } }
            scope.join().throwIfFailed()
            return taskA.get() to taskB.get()
        }
    }

    fun <A, B, C> execute(
        queryA: () -> A,
        queryB: () -> B,
        queryC: () -> C,
    ): Triple<A, B, C> {
        val resources = captureTransactionResources()
        StructuredTaskScope.ShutdownOnFailure().use { scope ->
            val taskA = scope.fork { withTransactionResources(resources) { queryA() } }
            val taskB = scope.fork { withTransactionResources(resources) { queryB() } }
            val taskC = scope.fork { withTransactionResources(resources) { queryC() } }
            scope.join().throwIfFailed()
            return Triple(taskA.get(), taskB.get(), taskC.get())
        }
    }

    private data class TransactionResources(
        val resourceMap: Map<Any, Any>,
        val synchronizationActive: Boolean,
        val currentTransactionName: String?,
        val currentTransactionReadOnly: Boolean,
        val actualTransactionActive: Boolean,
    )

    private fun captureTransactionResources(): TransactionResources {
        return TransactionResources(
            resourceMap = TransactionSynchronizationManager.getResourceMap(),
            synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive(),
            currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName(),
            currentTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
            actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive(),
        )
    }

    private fun <T> withTransactionResources(resources: TransactionResources, block: () -> T): T {
        resources.resourceMap.forEach { (key, value) ->
            TransactionSynchronizationManager.bindResource(key, value)
        }
        if (resources.synchronizationActive && !TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization()
        }
        TransactionSynchronizationManager.setCurrentTransactionName(resources.currentTransactionName)
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(resources.currentTransactionReadOnly)
        TransactionSynchronizationManager.setActualTransactionActive(resources.actualTransactionActive)
        try {
            return block()
        } finally {
            resources.resourceMap.keys.forEach { key ->
                TransactionSynchronizationManager.unbindResourceIfPossible(key)
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization()
            }
            TransactionSynchronizationManager.setCurrentTransactionName(null)
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
            TransactionSynchronizationManager.setActualTransactionActive(false)
        }
    }
}
