package kr.co.lokit.api.domain.photo.mapping

import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.domain.photo.domain.Emoticon
import kr.co.lokit.api.domain.photo.dto.CommentResponse
import kr.co.lokit.api.domain.photo.dto.EmoticonSummaryResponse
import kr.co.lokit.api.domain.photo.infrastructure.CommentEntity
import kr.co.lokit.api.domain.photo.infrastructure.EmoticonEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun Comment.toEntity(
    photo: PhotoEntity,
    user: UserEntity,
): CommentEntity =
    CommentEntity(
        photo = photo,
        user = user,
        content = this.content,
        commentedAt = this.commentedAt,
    )

fun CommentEntity.toDomain(): Comment =
    Comment(
        id = this.nonNullId(),
        photoId = this.photo.nonNullId(),
        userId = this.user.nonNullId(),
        content = this.content,
        commentedAt = this.commentedAt,
    )

fun Emoticon.toEntity(
    comment: CommentEntity,
    user: UserEntity,
): EmoticonEntity =
    EmoticonEntity(
        comment = comment,
        user = user,
        emoji = this.emoji,
    )

fun EmoticonEntity.toDomain(): Emoticon =
    Emoticon(
        id = this.nonNullId(),
        commentId = this.comment.nonNullId(),
        userId = this.user.nonNullId(),
        emoji = this.emoji,
    )

fun CommentWithEmoticons.toResponse(): CommentResponse =
    CommentResponse(
        id = this.comment.id,
        userId = this.comment.userId,
        userName = this.userName,
        userProfileImageUrl = this.userProfileImageUrl,
        content = this.comment.content,
        commentedAt = this.comment.commentedAt,
        emoticons = this.emoticons.map { EmoticonSummaryResponse(it.emoji, it.count, it.reacted) },
    )
