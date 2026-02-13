package kr.co.lokit.api.domain.photo.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.photo.application.port.`in`.CommentUseCase
import kr.co.lokit.api.domain.photo.application.port.`in`.EmoticonUseCase
import kr.co.lokit.api.domain.photo.domain.Comment
import kr.co.lokit.api.domain.photo.domain.Emoticon
import kr.co.lokit.api.domain.photo.dto.AddEmoticonRequest
import kr.co.lokit.api.domain.photo.dto.CommentListResponse
import kr.co.lokit.api.domain.photo.dto.CreateCommentRequest
import kr.co.lokit.api.domain.photo.dto.RemoveEmoticonRequest
import kr.co.lokit.api.domain.photo.mapping.toResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("photos")
class CommentController(
    private val commentUseCase: CommentUseCase,
    private val emoticonUseCase: EmoticonUseCase,
) : CommentApi {
    @PostMapping("{photoId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionService.canReadPhoto(#userId, #photoId)")
    override fun createComment(
        @CurrentUserId userId: Long,
        @PathVariable photoId: Long,
        @RequestBody @Valid request: CreateCommentRequest,
    ): IdResponse =
        commentUseCase
            .createComment(photoId, userId, request.content)
            .toIdResponse(Comment::id)

    @GetMapping("{photoId}/comments")
    @PreAuthorize("@permissionService.canReadPhoto(#userId, #photoId)")
    override fun getComments(
        @CurrentUserId userId: Long,
        @PathVariable photoId: Long,
    ): CommentListResponse =
        CommentListResponse(
            comments = commentUseCase.getComments(photoId, userId).map { it.toResponse() },
        )

    @PostMapping("comments/{commentId}/emoticons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@permissionService.canAccessComment(#userId, #commentId)")
    override fun addEmoticon(
        @CurrentUserId userId: Long,
        @PathVariable commentId: Long,
        @RequestBody @Valid request: AddEmoticonRequest,
    ): IdResponse =
        emoticonUseCase
            .addEmoticon(commentId, userId, request.emoji)
            .toIdResponse(Emoticon::id)

    @DeleteMapping("comments/{commentId}/emoticons")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionService.canAccessComment(#userId, #commentId)")
    override fun removeEmoticon(
        @CurrentUserId userId: Long,
        @PathVariable commentId: Long,
        @RequestBody @Valid request: RemoveEmoticonRequest,
    ) = emoticonUseCase.removeEmoticon(commentId, userId, request.emoji)
}
