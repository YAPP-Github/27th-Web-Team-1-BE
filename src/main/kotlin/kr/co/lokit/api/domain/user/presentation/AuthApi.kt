package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.dto.RefreshTokenRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Auth", description = "인증 API")
interface AuthApi {
    @Operation(
        summary = "토큰 갱신",
        description = "Refresh Token으로 새로운 Access Token을 발급받습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공",
                content = [Content(schema = Schema(implementation = JwtTokenResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Refresh Token",
                content = [Content()],
            ),
        ],
    )
    @SecurityRequirements
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): JwtTokenResponse

    @Operation(
        hidden = true,
        summary = "카카오 로그인 페이지로 리다이렉트",
        description = "카카오 OAuth 인증 페이지로 리다이렉트합니다. 프론트엔드에서 이 URL로 이동하면 카카오 로그인 화면이 표시됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "302",
                description = "카카오 인증 페이지로 리다이렉트",
            ),
        ],
    )
    @SecurityRequirements
    fun kakaoAuthorize(): ResponseEntity<Unit>
}
