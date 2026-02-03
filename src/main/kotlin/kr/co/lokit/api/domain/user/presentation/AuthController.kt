package kr.co.lokit.api.domain.user.presentation

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.config.web.CookieUtil
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.KakaoLoginService
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.dto.RefreshTokenRequest
import kr.co.lokit.api.domain.user.infrastructure.oauth.KakaoOAuthProperties
import kr.co.lokit.api.domain.user.mapping.toJwtTokenResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("auth")
class AuthController(
    private val authService: AuthService,
    private val kakaoLoginService: KakaoLoginService,
    private val kakaoOAuthProperties: KakaoOAuthProperties,
    private val cookieUtil: CookieUtil,
) : AuthApi {

    @PostMapping("refresh")
    override fun refresh(
        @RequestBody request: RefreshTokenRequest,
        req: HttpServletRequest,
    ): ResponseEntity<JwtTokenResponse> {
        val tokens = authService.refresh(request.refreshToken)

        val accessTokenCookie = cookieUtil.createAccessTokenCookie(req, tokens.accessToken)
        val refreshTokenCookie = cookieUtil.createRefreshTokenCookie(req, tokens.refreshToken)

        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(tokens.toJwtTokenResponse())
    }

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

        val accessTokenCookie = cookieUtil.createAccessTokenCookie(req, tokens.accessToken)
        val refreshTokenCookie = cookieUtil.createRefreshTokenCookie(req, tokens.refreshToken)

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .location(URI.create(kakaoOAuthProperties.frontRedirectUri))
            .build()
    }
}
