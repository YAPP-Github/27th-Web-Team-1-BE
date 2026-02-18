package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.co.lokit.api.domain.user.domain.AuthTokens

@Schema(description = "관리자 작업 공통 응답")
data class AdminActionResponse(
    @Schema(description = "작업 결과 메시지", example = "완료되었습니다.")
    val message: String,
)

@Schema(description = "관리자 사용자 조회 응답")
data class AdminUserSummaryResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @Schema(description = "사용자 이메일", example = "dev@example.com")
    val email: String,
)

@Schema(description = "관리자 커플 파트너 생성 응답")
data class AdminPartnerResponse(
    @Schema(description = "파트너 이메일", example = "1")
    val partnerEmail: String,
    val tokens: AuthTokens,
)
