package kr.co.lokit.api.config.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class CookieUtil(
    private val cookieProperties: CookieProperties,
    @Value("\${jwt.expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshTokenExpiration: Long,
) {

    fun createAccessTokenCookie(request: HttpServletRequest, value: String): ResponseCookie =
        createCookie(request, "accessToken", value, accessTokenExpiration)

    fun createRefreshTokenCookie(request: HttpServletRequest, value: String): ResponseCookie =
        createCookie(request, "refreshToken", value, refreshTokenExpiration)

    fun createCookie(request: HttpServletRequest, name: String, value: String, maxAgeMillis: Long): ResponseCookie {
        val builder = ResponseCookie
            .from(name, value)
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .path("/")
            .maxAge(maxAgeMillis / 1000)
            .sameSite(if (cookieProperties.secure) "None" else "Lax")

        if (cookieProperties.domains != null
            && cookieProperties.domains!!.isNotBlank()
            && !isLocalhost(request.serverName)
        ) {
            builder.domain(cookieProperties.domains)
        }

        return builder.build()
    }

    private fun isLocalhost(host: String?): Boolean {
        if (host.isNullOrBlank()) {
            return false
        }

        val normalizedHost = host
            .lowercase()
            .substringBefore(":")

        return normalizedHost == "localhost"
            || normalizedHost.endsWith(".localhost")
            || normalizedHost == "127.0.0.1"
            || normalizedHost == "::1"
    }
}
