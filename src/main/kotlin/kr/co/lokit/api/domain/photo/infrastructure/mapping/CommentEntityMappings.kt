package kr.co.lokit.api.domain.photo.infrastructure.mapping

import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.Emoticon
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
        content = content,
        commentedAt = commentedAt,
    )

fun CommentEntity.toDomain(): Comment =
    Comment(
        id = nonNullId(),
        photoId = photo.nonNullId(),
        userId = user.nonNullId(),
        content = content,
        commentedAt = commentedAt,
    )

fun Emoticon.toEntity(
    comment: CommentEntity,
    user: UserEntity,
): EmoticonEntity =
    EmoticonEntity(
        comment = comment,
        user = user,
        emoji = emoji,
    )

fun EmoticonEntity.toDomain(): Emoticon =
    Emoticon(
        id = nonNullId(),
        commentId = comment.nonNullId(),
        userId = user.nonNullId(),
        emoji = emoji,
    )
