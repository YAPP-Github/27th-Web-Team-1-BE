package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.co.lokit.api.domain.user.dto.AdminActionResponse
import kr.co.lokit.api.domain.user.dto.AdminInviteActionRequest
import kr.co.lokit.api.domain.user.dto.AdminInviteIssueRequest
import kr.co.lokit.api.domain.user.dto.AdminCoupleLinkRequest
import kr.co.lokit.api.domain.user.dto.AdminCoupleLinkResponse
import kr.co.lokit.api.domain.user.dto.AdminUserSummaryResponse
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse

@SecurityRequirement(name = "Authorization")
@Tag(name = "Admin", description = "개발/운영 지원 API")
interface AdminApi {
    @Operation(
        summary = "전체 사용자 목록 조회",
        description = "DB에 저장된 사용자 ID/이메일 목록을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "403", description = "관리자 키 불일치"),
        ],
    )
    fun getUsers(key: String): List<AdminUserSummaryResponse>

    @Operation(
        summary = "이메일 기준 사용자 데이터 전체 삭제",
        description = "사용자와 연결된 커플/앨범/사진/토큰 데이터를 함께 삭제합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "삭제 성공"),
            ApiResponse(responseCode = "403", description = "관리자 키 불일치"),
            ApiResponse(responseCode = "404", description = "대상 사용자 없음"),
        ],
    )
    fun deleteAllByEmail(
        email: String,
        key: String,
    ): AdminActionResponse

    @Operation(
        summary = "전체 캐시 강제 비우기",
        description = "서버 캐시를 즉시 비웁니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "처리 성공"),
            ApiResponse(responseCode = "403", description = "관리자 키 불일치"),
        ],
    )
    fun clearAllCaches(key: String): AdminActionResponse

    @Operation(
        summary = "개발용 커플 테스트 파트너 생성/연결",
        description = "현재 로그인 사용자 기준으로 파트너 유저 1명을 생성(또는 재사용)하고 커플에 연결한 뒤, 파트너 JWT를 발급합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "연결 성공"),
            ApiResponse(responseCode = "400", description = "입력 오류/연결 불가 상태"),
        ],
    )
    fun linkTestCouple(
        @Parameter(hidden = true) userId: Long,
        @Valid request: AdminCoupleLinkRequest,
    ): AdminCoupleLinkResponse

    @Operation(
        summary = "개발용 초대코드 발급",
        description = "지정한 사용자로 실제 초대코드 발급 유스케이스를 실행합니다.",
    )
    fun issueInviteCode(
        key: String,
        @Valid request: AdminInviteIssueRequest,
    ): InviteCodeResponse

    @Operation(
        summary = "개발용 초대코드 검증",
        description = "지정한 사용자로 실제 초대코드 검증 유스케이스를 실행합니다. 테스트용 clientIp는 서버 고정값(127.0.0.1)을 사용합니다.",
    )
    fun verifyInviteCode(
        key: String,
        @Valid request: AdminInviteActionRequest,
    ): InviteCodePreviewResponse

    @Operation(
        summary = "개발용 초대코드 확정",
        description = "지정한 사용자로 실제 초대코드 확정 유스케이스를 실행합니다. 테스트용 clientIp는 서버 고정값(127.0.0.1)을 사용합니다.",
    )
    fun confirmInviteCode(
        key: String,
        @Valid request: AdminInviteActionRequest,
    ): CoupleStatusResponse
}
