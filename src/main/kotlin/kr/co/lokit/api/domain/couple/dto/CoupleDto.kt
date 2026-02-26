package kr.co.lokit.api.domain.couple.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime

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
    @field:Size(min = 6, max = 6, message = "초대 코드가 잘못되었습니다.")
    @Schema(description = "초대 코드", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    val inviteCode: String,
)

@Schema(description = "초대 코드 응답")
data class InviteCodeResponse(
    val inviteCode: String,
    val expiresAt: LocalDateTime,
) {
    companion object {
        fun from(
            code: String,
            expiresAt: LocalDateTime,
        ): InviteCodeResponse = InviteCodeResponse(inviteCode = code, expiresAt = expiresAt)
    }
}

@Schema(description = "커플 상태 조회 응답")
data class CoupleStatusResponse(
    @get:JsonProperty("isCoupled")
    @field:Schema(name = "isCoupled", description = "커플 연결 여부", example = "true")
    val coupled: Boolean,
    val partnerSummary: PartnerSummaryResponse? = null,
)

@Schema(description = "파트너 요약 정보")
data class PartnerSummaryResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

@Schema(description = "초대코드 검증 요청")
data class VerifyInviteCodeRequest(
    @field:NotBlank(message = "초대 코드는 필수입니다")
    @field:Size(min = 6, max = 6, message = "초대 코드 형식이 잘못되었습니다.")
    @field:Pattern(regexp = "^\\d{6}$", message = "초대 코드 형식이 잘못되었습니다.")
    val inviteCode: String,
)

@Schema(description = "처음 만난 날짜 수정 요청")
data class UpdateFirstMetDateRequest(
    @field:NotNull(message = "처음 만난 날짜는 필수입니다")
    @Schema(description = "처음 만난 날짜", example = "2024-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    val firstMetDate: LocalDate,
)

@Schema(description = "초대코드 검증 응답")
data class InviteCodePreviewResponse(
    val inviterUserId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
