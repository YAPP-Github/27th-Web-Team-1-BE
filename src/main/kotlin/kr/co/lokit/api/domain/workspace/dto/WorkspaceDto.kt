package kr.co.lokit.api.domain.workspace.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "워크스페이스 생성 요청")
data class CreateWorkspaceRequest(
    @field:NotBlank(message = "워크스페이스 이름은 필수입니다")
    @field:Size(max = 20, message = "워크스페이스 이름은 20자 이하여야 합니다")
    @Schema(description = "워크스페이스 이름", example = "우리 가족")
    val name: String,
)

@Schema(description = "워크스페이스 합류 요청")
data class JoinWorkspaceRequest(
    @field:NotBlank(message = "초대 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "초대 코드는 8자여야 합니다")
    @Schema(description = "초대 코드", example = "ABC12345")
    val inviteCode: String,
)
