package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDate

data class Comment(
    val id: Long = 0L,
    val photoId: Long,
    val userId: Long,
    val content: String,
    val commentedAt: LocalDate = LocalDate.now(),
) {
    init {
        require(content.length <= 200) { "댓글은 200자 이내여야 합니다." }
    }
}
