package kr.co.lokit.api.domain.photo.application.port.`in`

import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons

interface CommentUseCase {
    fun createComment(photoId: Long, userId: Long, content: String): Comment

    fun getComments(photoId: Long, currentUserId: Long): List<CommentWithEmoticons>
}