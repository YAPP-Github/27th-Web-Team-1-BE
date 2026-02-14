package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Auth", description = "인증 API")
interface AuthApi {
    @Operation(
        summary = "카카오 로그인 페이지로 리다이렉트",
        description = "카카오 OAuth 인증 페이지로 리다이렉트합니다. redirect 파라미터로 로그인 후 돌아갈 프론트엔드 URL을 지정할 수 있습니다.",
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
    fun kakaoAuthorize(
        @Parameter(description = "로그인 후 리다이렉트할 프론트엔드 URL", example = "https://develop.lokit.co.kr")
        redirect: String?,
        @Parameter(hidden = true) req: HttpServletRequest,
    ): ResponseEntity<Unit>

    @Operation(hidden = true)
    fun kakaoCallback(
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
        @Parameter(hidden = true) req: HttpServletRequest,
    ): ResponseEntity<Unit>
}
