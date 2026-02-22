package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.common.permission.EditabilityPolicy
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.domain.photo.dto.CommentResponse
import kr.co.lokit.api.domain.photo.dto.EmoticonSummaryResponse

fun CommentWithEmoticons.toResponse(viewerUserId: Long): CommentResponse =
    CommentResponse(
        id = comment.id,
        userId = comment.userId,
        userName = userName,
        userProfileImageUrl = userProfileImageUrl,
        content = comment.content,
        commentedAt = comment.commentedAt,
        emoticons =
            emoticons.map {
                EmoticonSummaryResponse(
                    emoji = it.emoji,
                    count = it.count,
                    reacted = it.reacted,
                    isEditable = EditabilityPolicy.canEditEmoticon(it.reacted),
                )
            },
        isEditable =
            EditabilityPolicy.canEditOwnedResource(
                viewerUserId = viewerUserId,
                createdByUserId = comment.userId,
            ),
    )
