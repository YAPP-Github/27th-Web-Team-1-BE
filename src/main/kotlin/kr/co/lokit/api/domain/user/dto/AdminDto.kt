package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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

@Schema(description = "개발용 커플 연결 생성 요청")
data class AdminCoupleLinkRequest(
    @field:Email
    @Schema(
        description = "파트너 이메일(미입력 시 자동 생성)",
        example = "dev.partner@example.com",
        nullable = true,
    )
    val partnerEmail: String? = null,
    @field:Size(min = 1, max = 20)
    @Schema(
        description = "커플명(미입력 시 기본값 사용)",
        example = "테스트 커플",
        nullable = true,
    )
    val coupleName: String? = null,
)

@Schema(description = "개발용 커플 연결 생성 응답")
data class AdminCoupleLinkResponse(
    @Schema(description = "연결된 커플 ID", example = "10")
    val coupleId: Long,
    @Schema(description = "기준 사용자 ID", example = "1")
    val primaryUserId: Long,
    @Schema(description = "기준 사용자 이메일", example = "dev@example.com")
    val primaryEmail: String,
    @Schema(description = "파트너 사용자 ID", example = "2")
    val partnerUserId: Long,
    @Schema(description = "파트너 사용자 이메일", example = "dev.partner@example.com")
    val partnerEmail: String,
    @Schema(description = "처리 결과", example = "LINKED")
    val action: String,
    @Schema(description = "테스트 파트너 access token", example = "eyJhbGciOiJIUzI1NiJ9...")
    val partnerAccessToken: String,
    @Schema(description = "테스트 파트너 refresh token", example = "550e8400-e29b-41d4-a716-446655440000")
    val partnerRefreshToken: String,
)

@Schema(description = "개발용 초대코드 발급 요청")
data class AdminInviteIssueRequest(
    @field:NotBlank
    @field:Email
    @Schema(
        description = "초대코드를 발급할 사용자 이메일",
        example = "dev@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val issuerEmail: String,
)

@Schema(description = "개발용 초대코드 검증/확정 요청")
data class AdminInviteActionRequest(
    @field:NotBlank
    @field:Email
    @Schema(
        description = "동작 수행 사용자 이메일(검증자/참여자)",
        example = "dev.partner@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val actorEmail: String,
    @field:NotBlank
    @Schema(
        description = "초대 코드",
        example = "123456",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val inviteCode: String,
)
