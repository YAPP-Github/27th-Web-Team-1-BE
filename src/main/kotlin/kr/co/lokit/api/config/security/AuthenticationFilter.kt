package kr.co.lokit.api.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.user.application.AuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val compositeAuthenticationResolver: CompositeAuthenticationResolver,
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val cookieProperties: CookieProperties,
    @Value("\${jwt.expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshTokenExpiration: Long,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val accessToken = getTokenFromCookie(request, "accessToken")
            val refreshToken = getTokenFromCookie(request, "refreshToken")

            if (SecurityContextHolder.getContext().authentication != null) {
                filterChain.doFilter(request, response)
                return
            }

            // accessToken으로 인증 시도
            if (accessToken != null) {
                val authentication = compositeAuthenticationResolver.authenticate(accessToken)
                if (authentication != null) {
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    val securityContext = SecurityContextHolder.createEmptyContext()
                    securityContext.authentication = authentication
                    SecurityContextHolder.setContext(securityContext)
                    logger.debug("Authentication successful for user: ${authentication.name}")
                    filterChain.doFilter(request, response)
                    return
                }
            }

            // accessToken 실패/없음 → refreshToken으로 갱신 시도
            if (refreshToken != null) {
                val tokens = authService.refreshIfValid(refreshToken)
                if (tokens != null) {
                    // 새 토큰을 쿠키로 설정
                    setTokenCookies(response, tokens.accessToken, tokens.refreshToken)

                    // 새 accessToken으로 인증
                    val authentication = compositeAuthenticationResolver.authenticate(tokens.accessToken)
                    if (authentication != null) {
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        val securityContext = SecurityContextHolder.createEmptyContext()
                        securityContext.authentication = authentication
                        SecurityContextHolder.setContext(securityContext)
                        logger.debug("Token refreshed and authentication successful for user: ${authentication.name}")
                    }
                } else {
                    logger.debug("RefreshToken is invalid or expired")
                }
            } else {
                logger.debug("No tokens found in cookies")
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromCookie(request: HttpServletRequest, name: String): String? =
        request.cookies
            ?.find { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() && !it.contains(" ") }

    private fun setTokenCookies(response: HttpServletResponse, accessToken: String, refreshToken: String) {
        response.addHeader("Set-Cookie", createCookie("accessToken", accessToken, accessTokenExpiration).toString())
        response.addHeader("Set-Cookie", createCookie("refreshToken", refreshToken, refreshTokenExpiration).toString())
    }

    private fun createCookie(name: String, value: String, maxAge: Long): ResponseCookie {
        val builder = ResponseCookie
            .from(name, value)
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .path("/")
            .maxAge(maxAge / 1000)
            .sameSite(if (cookieProperties.secure) "None" else "Lax")

        if (cookieProperties.domains.isNotBlank()) {
            builder.domain(cookieProperties.domains)
        }

        return builder.build()
    }
}
