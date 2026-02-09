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
) : AuthApi {
    @ResponseStatus(HttpStatus.FOUND)
    @GetMapping("kakao")
    override fun kakaoAuthorize(
        @RequestParam(required = false) redirect: String?,
        req: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val resolvedRedirect = redirect ?: resolveRedirectFromReferer(req)
        val state =
            resolvedRedirect
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
                ?.takeIf { isAllowedRedirect(it) }
                ?: kakaoOAuthProperties.frontRedirectUri

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .location(URI.create(redirectUri))
            .build()
    }

    private fun resolveRedirectFromReferer(req: HttpServletRequest): String? {
        val referer = req.getHeader("Referer") ?: return null
        val uri = URI.create(referer)
        if (uri.host == LOCAL_HOST) {
            val port = if (uri.port > 0) ":${uri.port}" else ""
            return "${uri.scheme}://${uri.host}$port"
        }
        return null
    }

    private fun isAllowedRedirect(uri: String): Boolean =
        try {
            URI.create(uri).host?.endsWith(ALLOWED_DOMAIN) == true
        } catch (_: Exception) {
            false
        }

    companion object {
        private const val LOCAL_HOST = "local.lokit.co.kr"
        private const val ALLOWED_DOMAIN = "lokit.co.kr"
    }
}
