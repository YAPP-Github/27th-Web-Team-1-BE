package kr.co.lokit.api.domain.photo.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EmoticonJpaRepository : JpaRepository<EmoticonEntity, Long> {
    fun findAllByCommentIn(comments: List<CommentEntity>): List<EmoticonEntity>

    fun findByCommentIdAndUserIdAndEmoji(
        commentId: Long,
        userId: Long,
        emoji: String,
    ): EmoticonEntity?

    fun countByCommentIdAndUserId(
        commentId: Long,
        userId: Long,
    ): Long

    fun existsByCommentIdAndUserIdAndEmoji(
        commentId: Long,
        userId: Long,
        emoji: String,
    ): Boolean

    @Query(
        """
        select e.id
        from Emoticon e
        where e.user.id = :userId
            and e.comment.photo.album.couple.id in :coupleIds
        """,
    )
    fun findIdsByUserIdAndCoupleIds(
        userId: Long,
        coupleIds: Set<Long>,
    ): List<Long>

    @Query(
        """
        select e.id
        from Emoticon e
        where e.user.id = :userId
            and e.comment.id in :commentIds
        """,
    )
    fun findIdsByUserIdAndCommentIds(
        userId: Long,
        commentIds: Set<Long>,
    ): List<Long>

    @Query(
        """
        select e.id
        from Emoticon e
        where e.user.id = :userId
            and e.comment.photo.id in :photoIds
        """,
    )
    fun findIdsByUserIdAndPhotoIds(
        userId: Long,
        photoIds: Set<Long>,
    ): List<Long>
}
