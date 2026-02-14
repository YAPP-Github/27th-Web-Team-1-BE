package kr.co.lokit.api.domain.photo.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.photo.application.port.CommentRepositoryPort
import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.domain.photo.domain.EmoticonSummary
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toEntity
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class JpaCommentRepository(
    private val commentJpaRepository: CommentJpaRepository,
    private val emoticonJpaRepository: EmoticonJpaRepository,
    private val photoJpaRepository: PhotoJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : CommentRepositoryPort {
    override fun save(comment: Comment): Comment {
        val photoEntity =
            photoJpaRepository.findByIdOrNull(comment.photoId)
                ?: throw entityNotFound<Photo>(comment.photoId)
        val userEntity =
            userJpaRepository.findByIdOrNull(comment.userId)
                ?: throw entityNotFound<User>(comment.userId)
        val entity = comment.toEntity(photoEntity, userEntity)
        return commentJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): Comment {
        val entity =
            commentJpaRepository.findByIdOrNull(id)
                ?: throw entityNotFound<Comment>(id)
        return entity.toDomain()
    }

    override fun findAllByPhotoIdWithEmoticons(
        photoId: Long,
        currentUserId: Long,
    ): List<CommentWithEmoticons> {
        val comments = commentJpaRepository.findAllByPhotoId(photoId)
        if (comments.isEmpty()) return emptyList()

        val emoticons = emoticonJpaRepository.findAllByCommentIn(comments)
        val emoticonsByComment = emoticons.groupBy { it.comment.nonNullId() }

        return comments.map { comment ->
            val commentEmoticons = emoticonsByComment[comment.nonNullId()].orEmpty()
            val summaries =
                commentEmoticons
                    .groupBy { it.emoji }
                    .map { (emoji, group) ->
                        EmoticonSummary(
                            emoji = emoji,
                            count = group.size,
                            reacted = group.any { it.user.nonNullId() == currentUserId },
                        )
                    }
            CommentWithEmoticons(
                comment = comment.toDomain(),
                userName = comment.user.name,
                userProfileImageUrl = comment.user.profileImageUrl,
                emoticons = summaries,
            )
        }
    }
}
