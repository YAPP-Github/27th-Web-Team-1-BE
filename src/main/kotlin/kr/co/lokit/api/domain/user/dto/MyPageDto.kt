package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "닉네임 수정 요청")
data class UpdateNicknameRequest(
    @field:NotBlank
    @field:Size(max = 10)
    @Schema(description = "닉네임", example = "새닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
    val nickname: String,
)

@Schema(description = "프로필 사진 수정 요청")
data class UpdateProfileImageRequest(
    @field:NotBlank
    @Schema(description = "프로필 사진 URL", example = "https://example.com/profile.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    val profileImageUrl: String,
)
