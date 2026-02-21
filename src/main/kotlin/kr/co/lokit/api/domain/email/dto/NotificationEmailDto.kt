package kr.co.lokit.api.domain.email.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "알림 이메일 저장 요청")
data class SaveNotificationEmailRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효한 이메일 형식이어야 합니다.")
    @Schema(description = "알림 수신 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
)
