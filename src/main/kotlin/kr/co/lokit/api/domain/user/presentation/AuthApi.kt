package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.user.dto.AuthResponse
import kr.co.lokit.api.domain.user.dto.RegisterRequest
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Auth", description = "인증 관련 API")
interface AuthApi {
    @Operation(
        summary = "회원가입",
        description = "이메일과 이름으로 신규 사용자를 등록하고 JWT 토큰을 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content = [Content(schema = Schema(implementation = AuthResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효하지 않은 이메일 형식 등)",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "409",
                description = "이미 존재하는 이메일",
                content = [Content()],
            ),
        ],
    )
    @SecurityRequirements // 인증 불필요
    fun register(
        @RequestBody request: RegisterRequest,
    ): AuthResponse
}
