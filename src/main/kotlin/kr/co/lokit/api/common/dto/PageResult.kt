package kr.co.lokit.api.common.dto

import kotlin.math.ceil

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size).toInt()
    val isLast: Boolean = page >= totalPages - 1

    fun <R> map(transform: (T) -> R): PageResult<R> =
        PageResult(
            content = content.map(transform),
            page = page,
            size = size,
            totalElements = totalElements,
        )

    companion object {
        fun calculateOffset(page: Int, size: Int): Long = page.toLong() * size
    }
}