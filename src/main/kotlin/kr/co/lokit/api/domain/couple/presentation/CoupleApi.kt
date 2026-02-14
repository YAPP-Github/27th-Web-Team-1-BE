package kr.co.lokit.api.domain.couple.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.couple.dto.CoupleLinkResponse
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest
import kr.co.lokit.api.domain.couple.dto.VerifyInviteCodeRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Couple", description = "커플 API")
interface CoupleApi {
    @Operation(
        summary = "초대 코드 조회",
        description = "초대 코드를 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "커플 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 입력값"),
        ],
    )
    fun getCode(
        @Parameter(hidden = true) userId: Long,
    ): InviteCodeResponse

    @Operation(
        summary = "내 커플 상태 조회",
        description = "현재 로그인 사용자의 커플 연결 상태를 조회합니다.",
    )
    fun getMyStatus(
        @Parameter(hidden = true) userId: Long,
    ): CoupleStatusResponse

    @Operation(
        summary = "초대코드 생성",
        description = "초대코드를 생성하거나 활성 코드가 있으면 재사용합니다. 코드 만료 시간은 24시간입니다.",
    )
    fun createInvite(
        @Parameter(hidden = true) userId: Long,
    ): InviteCodeResponse

    @Operation(
        summary = "초대코드 갱신",
        description = "기존 활성 코드를 폐기하고 새 코드를 재발급합니다. 만료 시간은 24시간입니다.",
    )
    fun refreshInvite(
        @Parameter(hidden = true) userId: Long,
    ): InviteCodeResponse

    @Operation(
        summary = "초대코드 철회",
        description = "본인이 생성한 UNUSED 코드만 철회합니다.",
    )
    fun revokeInvite(
        inviteCode: String,
        @Parameter(hidden = true) userId: Long,
    )

    @Operation(
        summary = "초대코드 검증",
        description = "코드 유효성을 확인하고 초대자 최소 정보를 미리보기로 제공합니다.",
    )
    fun verifyInviteCode(
        request: VerifyInviteCodeRequest,
        @Parameter(hidden = true) userId: Long,
        @Parameter(hidden = true) httpRequest: HttpServletRequest,
    ): InviteCodePreviewResponse

    @Operation(
        summary = "커플 연결 확정",
        description = "초대코드를 사용해 커플 연결을 확정합니다.",
    )
    fun confirmInviteCode(
        request: JoinCoupleRequest,
        @Parameter(hidden = true) userId: Long,
        @Parameter(hidden = true) httpRequest: HttpServletRequest,
    ): CoupleLinkResponse

    @Operation(
        summary = "초대 코드로 커플 합류",
        description = "초대 코드를 통해 커플에 합류합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "커플 합류 성공"),
            ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드"),
        ],
    )
    fun joinByInviteCode(
        request: JoinCoupleRequest,
        @Parameter(hidden = true) userId: Long,
        @Parameter(hidden = true) httpRequest: HttpServletRequest,
    ): IdResponse

    @Operation(
        summary = "커플 재연결",
        description = "연결 해제된 커플에 재연결합니다. 연결 해제 후 31일 이내이며 기존 커플에 최소 1명이 잔존한 경우에만 가능합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "재연결 성공"),
            ApiResponse(responseCode = "400", description = "연결 해제 상태가 아닌 커플"),
            ApiResponse(responseCode = "403", description = "재연결 불가 상태"),
            ApiResponse(responseCode = "410", description = "재연결 가능 기간 만료"),
        ],
    )
    fun reconnect(
        @Parameter(hidden = true) userId: Long,
    ): IdResponse

    @Operation(
        summary = "커플 연결 끊기",
        description = "현재 커플 연결을 해제합니다. 31일 이내 재연결이 가능합니다.",
        responses = [
            ApiResponse(responseCode = "204", description = "연결 끊기 성공"),
            ApiResponse(responseCode = "404", description = "커플을 찾을 수 없음"),
            ApiResponse(responseCode = "409", description = "이미 연결이 해제된 커플"),
        ],
    )
    fun disconnect(
        @Parameter(hidden = true) userId: Long,
    )
}
