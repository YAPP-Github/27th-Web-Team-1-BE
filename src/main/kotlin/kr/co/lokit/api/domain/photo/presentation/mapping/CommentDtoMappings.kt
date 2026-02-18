package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons
import kr.co.lokit.api.domain.photo.dto.CommentResponse
import kr.co.lokit.api.domain.photo.dto.EmoticonSummaryResponse

fun CommentWithEmoticons.toResponse(): CommentResponse =
    CommentResponse(
        id = comment.id,
        userId = comment.userId,
        userName = userName,
        userProfileImageUrl = userProfileImageUrl,
        content = comment.content,
        commentedAt = comment.commentedAt,
        emoticons = emoticons.map { EmoticonSummaryResponse(it.emoji, it.count, it.reacted) },
    )
