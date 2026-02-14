package kr.co.lokit.api.domain.photo.domain

data class Emoticon(
    val id: Long = 0L,
    val commentId: Long,
    val userId: Long,
    val emoji: String,
) {
    companion object {
        const val MAX_PER_USER_PER_COMMENT = 10
    }
}
