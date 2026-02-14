package kr.co.lokit.api.config.logging

import java.util.concurrent.ConcurrentLinkedQueue

object RequestTrace {
    private val traces = InheritableThreadLocal<ConcurrentLinkedQueue<TraceHistory>>()

    data class TraceHistory(
        val method: String,
        val durationMs: Long,
    )

    fun init() {
        traces.set(ConcurrentLinkedQueue())
    }

    fun add(
        method: String,
        durationMs: Long,
    ) {
        traces.get()?.add(TraceHistory(method, durationMs))
    }

    fun snapshot(): List<TraceHistory> = traces.get()?.toList().orEmpty()

    fun drain(): List<TraceHistory> {
        val queue = traces.get() ?: return emptyList()
        traces.remove()
        return queue.toList()
    }
}
