package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.user.dto.AdminActionResponse
import kr.co.lokit.api.domain.user.dto.AdminCoupleMigrationResponse
import kr.co.lokit.api.domain.user.dto.AdminPartnerResponse
import kr.co.lokit.api.domain.user.dto.AdminUserSummaryResponse

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
    @SecurityRequirements
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
    @SecurityRequirements
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
    @SecurityRequirements
    fun clearAllCaches(key: String): AdminActionResponse

    @Operation(
        summary = "개발용 커플 테스트 파트너 생성",
        description = "파트너 유저 1명을 생성(또는 재사용)하고 파트너 JWT를 발급합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
            ApiResponse(responseCode = "400", description = "입력 오류/연결 불가 상태"),
        ],
    )
    @SecurityRequirement(name = "Authorization")
    fun createCouplePartner(
        @Parameter(hidden = true) userId: Long,
    ): AdminPartnerResponse

    @Operation(
        summary = "현재 사용자 기준 이전 커플 데이터 이관",
        description = "관리자 키와 사용자 JWT를 이용해 사용자의 과거 커플 데이터(본인 소유 리소스)를 현재 커플로 이관합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "이관 성공"),
            ApiResponse(responseCode = "403", description = "관리자 키 불일치"),
            ApiResponse(responseCode = "404", description = "사용자/커플/기본앨범 미존재"),
        ],
    )
    @SecurityRequirement(name = "Authorization")
    fun migratePreviousCoupleData(
        @Parameter(hidden = true) userId: Long,
        key: String,
    ): AdminCoupleMigrationResponse
}
