package kr.co.lokit.api.domain.photo.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "댓글 생성 요청")
data class CreateCommentRequest(
    @field:NotBlank(message = "댓글 내용은 필수입니다.")
    @field:Size(max = 200, message = "댓글은 200자 이내여야 합니다.")
    @Schema(description = "댓글 내용", example = "멋진 사진이네요!", requiredMode = Schema.RequiredMode.REQUIRED)
    val content: String,
)

@Schema(description = "이모지 추가 요청")
data class AddEmoticonRequest(
    @field:NotBlank(message = "이모지는 필수입니다.")
    @Schema(description = "이모지", example = "❤️", requiredMode = Schema.RequiredMode.REQUIRED)
    val emoji: String,
)

@Schema(description = "이모지 제거 요청")
data class RemoveEmoticonRequest(
    @field:NotBlank(message = "이모지는 필수입니다.")
    @Schema(description = "이모지", example = "❤️", requiredMode = Schema.RequiredMode.REQUIRED)
    val emoji: String,
)

@Schema(description = "이모지 요약 정보")
data class EmoticonSummaryResponse(
    @Schema(description = "이모지", example = "❤️")
    val emoji: String,
    @Schema(description = "이모지 개수", example = "3")
    val count: Int,
    @Schema(description = "현재 사용자가 반응했는지 여부", example = "true")
    val reacted: Boolean,
    @Schema(description = "조회자가 해당 이모티콘 반응을 제거할 수 있는지 여부", example = "true")
    @field:JsonProperty("isEditable")
    val isEditable: Boolean,
)

@Schema(description = "댓글 정보")
data class CommentResponse(
    @Schema(description = "댓글 ID", example = "1")
    val id: Long,
    @Schema(description = "작성자 ID", example = "1")
    val userId: Long,
    @Schema(description = "작성자 이름", example = "홍길동")
    val userName: String,
    @Schema(description = "작성자 프로필 이미지 URL")
    val userProfileImageUrl: String?,
    @Schema(description = "댓글 내용", example = "멋진 사진이네요!")
    val content: String,
    @Schema(description = "작성일")
    val commentedAt: LocalDate,
    @Schema(description = "이모지 목록")
    val emoticons: List<EmoticonSummaryResponse>,
    @Schema(description = "조회자가 해당 댓글을 수정/삭제할 수 있는지 여부", example = "true")
    @field:JsonProperty("isEditable")
    val isEditable: Boolean,
)

@Schema(description = "댓글 목록 응답")
data class CommentListResponse(
    @Schema(description = "댓글 목록")
    val comments: List<CommentResponse>,
)
