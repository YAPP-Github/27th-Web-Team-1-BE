package kr.co.lokit.api.domain.photo.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.photo.dto.AddEmoticonRequest
import kr.co.lokit.api.domain.photo.dto.CommentListResponse
import kr.co.lokit.api.domain.photo.dto.CreateCommentRequest
import kr.co.lokit.api.domain.photo.dto.RemoveEmoticonRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Comment", description = "댓글 & 이모지 API")
interface CommentApi {
    @Operation(
        summary = "댓글 생성",
        description = "사진에 댓글을 작성합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요", content = [Content()]),
            ApiResponse(responseCode = "403", description = "접근 권한 없음", content = [Content()]),
            ApiResponse(responseCode = "404", description = "사진을 찾을 수 없음", content = [Content()]),
        ],
    )
    fun createComment(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "사진 ID", example = "1", required = true) photoId: Long,
        request: CreateCommentRequest,
    ): IdResponse

    @Operation(
        summary = "댓글 목록 조회",
        description = "사진의 댓글 목록을 이모지 정보와 함께 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = CommentListResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "인증 필요", content = [Content()]),
            ApiResponse(responseCode = "403", description = "접근 권한 없음", content = [Content()]),
        ],
    )
    fun getComments(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "사진 ID", example = "1", required = true) photoId: Long,
    ): CommentListResponse

    @Operation(
        summary = "이모지 추가",
        description = "댓글에 이모지를 추가합니다. 사용자당 댓글당 최대 10개의 서로 다른 이모지를 추가할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "이모지 추가 성공"),
            ApiResponse(responseCode = "400", description = "이모지 최대 개수 초과 (COMMENT_001)", content = [Content()]),
            ApiResponse(responseCode = "401", description = "인증 필요", content = [Content()]),
            ApiResponse(responseCode = "403", description = "접근 권한 없음", content = [Content()]),
            ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음", content = [Content()]),
            ApiResponse(responseCode = "409", description = "이미 추가된 이모지", content = [Content()]),
        ],
    )
    fun addEmoticon(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "댓글 ID", example = "1", required = true) commentId: Long,
        request: AddEmoticonRequest,
    ): IdResponse

    @Operation(
        summary = "이모지 제거",
        description = "댓글에서 이모지를 제거합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "이모지 제거 성공"),
            ApiResponse(responseCode = "401", description = "인증 필요", content = [Content()]),
            ApiResponse(responseCode = "403", description = "접근 권한 없음", content = [Content()]),
            ApiResponse(responseCode = "404", description = "이모지를 찾을 수 없음 (COMMENT_002)", content = [Content()]),
        ],
    )
    fun removeEmoticon(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "댓글 ID", example = "1", required = true) commentId: Long,
        request: RemoveEmoticonRequest,
    )
}
