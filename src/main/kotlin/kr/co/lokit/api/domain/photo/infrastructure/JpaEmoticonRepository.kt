package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.photo.application.port.EmoticonRepositoryPort
import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.Emoticon
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaEmoticonRepository(
    private val emoticonJpaRepository: EmoticonJpaRepository,
    private val commentJpaRepository: CommentJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : EmoticonRepositoryPort {
    override fun save(emoticon: Emoticon): Emoticon {
        val commentEntity = commentJpaRepository.findByIdOrNull(emoticon.commentId)
            ?: throw entityNotFound<Comment>(emoticon.commentId)
        val userEntity = userJpaRepository.findByIdOrNull(emoticon.userId)
            ?: throw entityNotFound<User>(emoticon.userId)
        val entity = emoticon.toEntity(commentEntity, userEntity)
        return emoticonJpaRepository.save(entity).toDomain()
    }

    override fun delete(commentId: Long, userId: Long, emoji: String) {
        val entity = emoticonJpaRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
            ?: throw entityNotFound<Emoticon>("commentId", commentId)
        emoticonJpaRepository.delete(entity)
    }

    override fun countByCommentIdAndUserId(commentId: Long, userId: Long): Long =
        emoticonJpaRepository.countByCommentIdAndUserId(commentId, userId)

    override fun existsByCommentIdAndUserIdAndEmoji(commentId: Long, userId: Long, emoji: String): Boolean =
        emoticonJpaRepository.existsByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
}