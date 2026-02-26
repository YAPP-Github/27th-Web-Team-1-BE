package kr.co.lokit.api.config.web

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.constants.CoupleCookieStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class CookieGenerator(
    private val cookieProperties: CookieProperties,
    @Value("\${jwt.expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshTokenExpiration: Long,
) {
    fun createAccessTokenCookie(
        request: HttpServletRequest,
        value: String,
    ): ResponseCookie = createCookie(request, "accessToken", value, accessTokenExpiration)

    fun createRefreshTokenCookie(
        request: HttpServletRequest,
        value: String,
    ): ResponseCookie = createCookie(request, "refreshToken", value, refreshTokenExpiration)

    fun clearAccessTokenCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, "accessToken", "", 0)

    fun clearRefreshTokenCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, "refreshToken", "", 0)

    fun createCoupleStatusCookie(
        request: HttpServletRequest,
        value: CoupleCookieStatus,
    ): ResponseCookie = createCookie(request, "coupleStatus", value.name, refreshTokenExpiration, httpOnly = false)

    fun clearCoupleStatusCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, "coupleStatus", "", 0, httpOnly = false)

    fun createCookie(
        request: HttpServletRequest,
        name: String,
        value: String,
        maxAgeMillis: Long,
        httpOnly: Boolean = true,
    ): ResponseCookie {
        val serverName = request.serverName
        val isLocal = isLocalhost(serverName)

        val builder =
            ResponseCookie
                .from(name, value)
                .httpOnly(httpOnly)
                .path("/")
                .maxAge(maxAgeMillis / 1000)
                .secure(!isLocal && cookieProperties.secure)
                .sameSite(if (isLocal) "Lax" else "None")

        val domain = getCookieDomain(serverName)
        if (!isLocal && !domain.isNullOrBlank()) {
            builder.domain(domain)
        }

        return builder.build()
    }

    private fun getCookieDomain(serverName: String?): String? {
        if (serverName.isNullOrBlank() || isLocalhost(serverName)) {
            return null
        }
        return cookieProperties.domain
    }

    private fun isLocalhost(host: String): Boolean {
        val normalizedHost = host.lowercase().substringBefore(":")
        return normalizedHost == "localhost" ||
            normalizedHost.endsWith(".localhost") ||
            normalizedHost == "127.0.0.1" ||
            normalizedHost == "::1"
    }
}
