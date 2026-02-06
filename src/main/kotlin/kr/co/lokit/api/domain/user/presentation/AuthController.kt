package kr.co.lokit.api.domain.user.presentation

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CorsProperties
import kr.co.lokit.api.domain.user.application.KakaoLoginService
import kr.co.lokit.api.domain.user.infrastructure.oauth.KakaoOAuthProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("auth")
class AuthController(
    private val kakaoLoginService: KakaoLoginService,
    private val kakaoOAuthProperties: KakaoOAuthProperties,
    private val cookieGenerator: CookieGenerator,
    private val corsProperties: CorsProperties,
) : AuthApi {
    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("kakao")
    override fun kakaoAuthorize(
        @RequestParam(required = false) redirect: String?,
    ): ResponseEntity<Unit> {
        val state =
            redirect
                ?.takeIf { isAllowedOrigin(it) }
                ?.let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
                ?: ""

        val authUrl =
            KakaoOAuthProperties.AUTHORIZATION_URL +
                "?client_id=${kakaoOAuthProperties.clientId}" +
                "&redirect_uri=${kakaoOAuthProperties.redirectUri}" +
                "&response_type=code" +
                "&state=$state"

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(authUrl))
            .build()
    }

    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("kakao/callback")
    override fun kakaoCallback(
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
        req: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val tokens = kakaoLoginService.login(code)

        val accessTokenCookie = cookieGenerator.createAccessTokenCookie(req, tokens.accessToken)
        val refreshTokenCookie = cookieGenerator.createRefreshTokenCookie(req, tokens.refreshToken)

        val redirectUri =
            state
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                ?.takeIf { isAllowedOrigin(it) }
                ?: kakaoOAuthProperties.frontRedirectUri

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .location(URI.create(redirectUri))
            .build()
    }

    private fun isAllowedOrigin(uri: String): Boolean =
        try {
            val parsed = URI(uri)
            val origin = "${parsed.scheme}://${parsed.authority}"
            corsProperties.allowedOrigins.any { it == origin || it == uri }
        } catch (_: Exception) {
            false
        }
}
