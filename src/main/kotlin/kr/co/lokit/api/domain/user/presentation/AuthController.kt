package kr.co.lokit.api.domain.user.presentation

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.user.application.KakaoLoginService
import kr.co.lokit.api.domain.user.infrastructure.oauth.KakaoOAuthProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("auth")
class AuthController(
    private val kakaoLoginService: KakaoLoginService,
    private val kakaoOAuthProperties: KakaoOAuthProperties,
    private val cookieGenerator: CookieGenerator,
) : AuthApi {

    @GetMapping("kakao")
    override fun kakaoAuthorize(): ResponseEntity<Unit> {
        val authUrl =
            KakaoOAuthProperties.AUTHORIZATION_URL +
                "?client_id=${kakaoOAuthProperties.clientId}" +
                "&redirect_uri=${kakaoOAuthProperties.redirectUri}" +
                "&response_type=code"

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(authUrl))
            .build()
    }

    @GetMapping("kakao/callback")
    override fun kakaoCallback(
        @RequestParam code: String,
        req: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val tokens = kakaoLoginService.login(code)

        val accessTokenCookie = cookieGenerator.createAccessTokenCookie(req, tokens.accessToken)
        val refreshTokenCookie = cookieGenerator.createRefreshTokenCookie(req, tokens.refreshToken)

        val redirectUri = req.getHeader("Origin")
            ?: req.getHeader("Referer")?.let { URI(it).scheme + "://" + URI(it).authority }
            ?: kakaoOAuthProperties.frontRedirectUri

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .location(URI.create(redirectUri))
            .build()
    }
}
