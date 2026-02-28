package kr.co.lokit.api.domain.user.presentation

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.user.application.port.`in`.WithdrawUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("users")
class UserController(
    private val withdrawUseCase: WithdrawUseCase,
    private val cookieGenerator: CookieGenerator,
) : UserApi {
    @DeleteMapping("me")
    override fun withdraw(
        @CurrentUserId userId: Long,
        request: HttpServletRequest,
    ): ResponseEntity<Unit> {
        withdrawUseCase.withdraw(userId)

        val clearAccessToken = cookieGenerator.clearAccessTokenCookie(request)
        val clearRefreshToken = cookieGenerator.clearRefreshTokenCookie(request)
        val clearCoupleStatus = cookieGenerator.clearCoupleStatusCookie(request)

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .header(HttpHeaders.SET_COOKIE, clearAccessToken.toString())
            .header(HttpHeaders.SET_COOKIE, clearRefreshToken.toString())
            .header(HttpHeaders.SET_COOKIE, clearCoupleStatus.toString())
            .build()
    }
}
