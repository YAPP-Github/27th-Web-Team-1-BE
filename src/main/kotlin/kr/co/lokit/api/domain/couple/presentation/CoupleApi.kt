package kr.co.lokit.api.domain.couple.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.couple.dto.CreateCoupleRequest
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Couple", description = "커플 API")
interface CoupleApi {
    @Operation(
        summary = "커플 생성",
        description = "새로운 커플을 생성합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "커플 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 입력값"),
        ],
    )
    fun create(
        request: CreateCoupleRequest,
        @Parameter(hidden = true) userId: Long,
    ): IdResponse

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
    ): IdResponse
}
