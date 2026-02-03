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

    @Operation(hidden = true)
    fun kakaoCallback(
        @RequestParam code: String,
        @Parameter(hidden = true) req: HttpServletRequest,
    ): ResponseEntity<Unit>
}
