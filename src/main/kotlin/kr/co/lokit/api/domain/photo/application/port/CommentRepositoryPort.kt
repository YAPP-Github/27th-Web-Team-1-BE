package kr.co.lokit.api.domain.photo.application.port

import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.CommentWithEmoticons

interface CommentRepositoryPort {
    fun save(comment: Comment): Comment

    fun findById(id: Long): Comment

    fun findAllByPhotoIdWithEmoticons(
        photoId: Long,
        currentUserId: Long,
    ): List<CommentWithEmoticons>
}
