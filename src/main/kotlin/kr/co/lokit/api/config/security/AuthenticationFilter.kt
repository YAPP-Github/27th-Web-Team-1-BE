package kr.co.lokit.api.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.user.application.AuthService
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val compositeAuthenticationResolver: CompositeAuthenticationResolver,
    private val authService: AuthService,
    private val cookieGenerator: CookieGenerator,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val accessToken = getTokenFromCookie(request, "accessToken")
            val refreshToken = getTokenFromCookie(request, "refreshToken")

            log.debug(
                "Auth filter: uri={}, accessToken={}, refreshToken={}, cookies={}",
                request.requestURI,
                if (accessToken != null) "present(${accessToken.take(20)}...)" else "null",
                if (refreshToken != null) "present(${refreshToken.take(20)}...)" else "null",
                request.cookies?.map { "${it.name}=${it.value.take(10)}..." }?.joinToString(", ") ?: "none",
            )

            if (SecurityContextHolder.getContext().authentication != null) {
                filterChain.doFilter(request, response)
                return
            }

            if (accessToken != null) {
                val authentication = compositeAuthenticationResolver.authenticate(accessToken)
                if (authentication != null) {
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    val securityContext = SecurityContextHolder.createEmptyContext()
                    securityContext.authentication = authentication
                    SecurityContextHolder.setContext(securityContext)
                    log.debug("Authentication successful for user: ${authentication.name}")
                    filterChain.doFilter(request, response)
                    return
                } else {
                    log.debug("AccessToken validation failed")
                }
            }

            if (refreshToken != null) {
                val tokens = authService.refreshIfValid(refreshToken)
                if (tokens != null) {
                    setTokenCookies(request, response, tokens.accessToken, tokens.refreshToken)

                    val authentication = compositeAuthenticationResolver.authenticate(tokens.accessToken)
                    if (authentication != null) {
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        val securityContext = SecurityContextHolder.createEmptyContext()
                        securityContext.authentication = authentication
                        SecurityContextHolder.setContext(securityContext)
                        log.debug("Token refreshed and authentication successful for user: ${authentication.name}")
                    }
                } else {
                    log.debug("RefreshToken is invalid or expired")
                }
            } else {
                log.debug("No tokens found in cookies")
            }
        } catch (e: Exception) {
            log.error("Cannot set user authentication: ${e.message}", e)
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromCookie(
        request: HttpServletRequest,
        name: String,
    ): String? =
        request.cookies
            ?.find { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() && !it.contains(" ") }

    private fun setTokenCookies(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessToken: String,
        refreshToken: String,
    ) {
        response.addHeader("Set-Cookie", cookieGenerator.createAccessTokenCookie(request, accessToken).toString())
        response.addHeader("Set-Cookie", cookieGenerator.createRefreshTokenCookie(request, refreshToken).toString())
    }
}
