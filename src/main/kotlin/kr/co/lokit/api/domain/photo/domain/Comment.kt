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
        require(content.length <= 500) { "댓글은 500자 이내여야 합니다." } // 최대 길이 수정 필요
    }
}
