package kr.co.lokit.api.domain.photo.application.port

import kr.co.lokit.api.domain.photo.domain.Emoticon

interface EmoticonRepositoryPort {
    fun save(emoticon: Emoticon): Emoticon

    fun delete(
        commentId: Long,
        userId: Long,
        emoji: String,
    )

    fun countByCommentIdAndUserId(
        commentId: Long,
        userId: Long,
    ): Long

    fun existsByCommentIdAndUserIdAndEmoji(
        commentId: Long,
        userId: Long,
        emoji: String,
    ): Boolean

    fun findIdsByUserIdAndCoupleIds(
        userId: Long,
        coupleIds: Set<Long>,
    ): Set<Long>

    fun findIdsByUserIdAndPhotoIds(
        userId: Long,
        photoIds: Set<Long>,
    ): Set<Long>
}
