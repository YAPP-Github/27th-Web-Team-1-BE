package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface EmoticonJpaRepository : JpaRepository<EmoticonEntity, Long> {
    fun findAllByCommentIn(comments: List<CommentEntity>): List<EmoticonEntity>

    fun findByCommentIdAndUserIdAndEmoji(commentId: Long, userId: Long, emoji: String): EmoticonEntity?

    fun countByCommentIdAndUserId(commentId: Long, userId: Long): Long

    fun existsByCommentIdAndUserIdAndEmoji(commentId: Long, userId: Long, emoji: String): Boolean
}