package kr.co.lokit.api.domain.user.presentation

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorCode
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.couple.application.CoupleCookieStatusResolver
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.LoginService
import kr.co.lokit.api.domain.user.infrastructure.oauth.KakaoOAuthProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("auth")
class AuthController(
    private val loginService: LoginService,
    private val authService: AuthService,
    private val kakaoOAuthProperties: KakaoOAuthProperties,
    private val coupleCookieStatusResolver: CoupleCookieStatusResolver,
    private val cookieGenerator: CookieGenerator,
    @Value("\${redirect.local-host}") private val localHostRedirect: String,
    @Value("\${redirect.allowed-domain}") private val allowedDomain: String,
) : AuthApi {
    private val log = LoggerFactory.getLogger(javaClass)

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
                .orEmpty()

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
        val redirectUri =
            state
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                ?.takeIf { isAllowedRedirect(it) }
                ?: kakaoOAuthProperties.frontRedirectUri

        return try {
            val loginResult = loginService.login(code)
            val accessTokenCookie = cookieGenerator.createAccessTokenCookie(req, loginResult.tokens.accessToken)
            val refreshTokenCookie = cookieGenerator.createRefreshTokenCookie(req, loginResult.tokens.refreshToken)
            val coupleStatusCookie =
                cookieGenerator.createCoupleStatusCookie(req, coupleCookieStatusResolver.resolve(loginResult.userId))

            ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, coupleStatusCookie.toString())
                .location(URI.create(redirectUri))
                .build()
        } catch (ex: BusinessException) {
            log.info("Kakao callback failed: {}", ex.errorCode.code)
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(buildErrorRedirectUri(redirectUri, ex.errorCode.code)))
                .build()
        } catch (ex: Exception) {
            log.error("Kakao callback unexpected error", ex)
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(buildErrorRedirectUri(redirectUri, ErrorCode.INTERNAL_SERVER_ERROR.code)))
                .build()
        }
    }

    @PostMapping("logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun logout(
        @CurrentUserId userId: Long,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ) {
        authService.logout(userId)
        res.addHeader(HttpHeaders.SET_COOKIE, cookieGenerator.clearAccessTokenCookie(req).toString())
        res.addHeader(HttpHeaders.SET_COOKIE, cookieGenerator.clearRefreshTokenCookie(req).toString())
        res.addHeader(HttpHeaders.SET_COOKIE, cookieGenerator.clearCoupleStatusCookie(req).toString())
    }

    private fun resolveRedirectFromReferer(req: HttpServletRequest): String? {
        val referer = req.getHeader("Referer") ?: return null
        val uri = URI.create(referer)
        if (uri.host == localHostRedirect) {
            val port = if (uri.port > 0) ":${uri.port}" else ""
            return "${uri.scheme}://${uri.host}$port"
        }
        return null
    }

    private fun isAllowedRedirect(uri: String): Boolean =
        try {
            URI.create(uri).host?.endsWith(allowedDomain) == true
        } catch (_: Exception) {
            false
        }

    private fun buildErrorRedirectUri(
        redirectUri: String,
        errorCode: String,
    ): String =
        UriComponentsBuilder
            .fromUriString(redirectUri)
            .queryParam("oauth_error", errorCode)
            .build(true)
            .toUriString()
}
