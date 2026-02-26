package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

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
    @Schema(
        description = "프로필 사진 URL",
        example = "https://example.com/profile.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val profileImageUrl: String,
)

@Schema(description = "마이페이지 조회 응답")
data class MyPageResponse(
    @Schema(description = "내 이메일", example = "user@example.com")
    val myEmail: String,
    @Schema(description = "내 닉네임", example = "홍로킷")
    val myName: String,
    @Schema(description = "내 프로필 이미지 URL", example = "https://example.com/me.jpg")
    val myProfileImageUrl: String?,
    @Schema(description = "상대방 닉네임", example = "수로킷")
    val partnerName: String?,
    @Schema(description = "상대방 프로필 이미지 URL", example = "https://example.com/partner.jpg")
    val partnerProfileImageUrl: String?,
    @Schema(description = "처음 만난 날짜 (미설정 시 null)", example = "2024-11-09")
    val firstMetDate: LocalDate?,
    @Schema(description = "처음 만난 날짜 기준 D-Day (미설정 시 null)", example = "100")
    val coupledDay: Long?,
    @Schema(description = "커플 내 전체 사진 수", example = "128")
    val couplePhotoCount: Long,
)
