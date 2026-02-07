package kr.co.lokit.api.domain.couple.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "커플 생성 요청")
data class CreateCoupleRequest(
    @field:NotBlank(message = "커플 이름은 필수입니다")
    @field:Size(max = 20, message = "커플 이름은 20자 이하여야 합니다")
    @Schema(description = "커플 이름", example = "우리 커플", requiredMode = Schema.RequiredMode.REQUIRED)
    val name: String,
)

@Schema(description = "커플 합류 요청")
data class JoinCoupleRequest(
    @field:NotBlank(message = "초대 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "초대 코드가 잘못되었습니다.")
    @Schema(description = "초대 코드", example = "ABC12345", requiredMode = Schema.RequiredMode.REQUIRED)
    val inviteCode: String,
)
