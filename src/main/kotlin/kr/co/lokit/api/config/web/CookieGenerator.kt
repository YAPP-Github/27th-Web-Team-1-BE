package kr.co.lokit.api.config.web

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.constants.CoupleCookieStatus
import kr.co.lokit.api.common.constants.DomainCookie
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
    ): ResponseCookie = createCookie(request, DomainCookie.ACCESS_TOKEN, value, accessTokenExpiration)

    fun createRefreshTokenCookie(
        request: HttpServletRequest,
        value: String,
    ): ResponseCookie = createCookie(request, DomainCookie.REFRESH_TOKEN, value, refreshTokenExpiration)

    fun clearAccessTokenCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, DomainCookie.ACCESS_TOKEN, "", 0)

    fun clearRefreshTokenCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, DomainCookie.REFRESH_TOKEN, "", 0)

    fun createCoupleStatusCookie(
        request: HttpServletRequest,
        value: CoupleCookieStatus,
    ): ResponseCookie =
        createCookie(request, DomainCookie.COUPLE_STATUS, value.name, refreshTokenExpiration, httpOnly = false)

    fun clearCoupleStatusCookie(request: HttpServletRequest): ResponseCookie =
        createCookie(request, DomainCookie.COUPLE_STATUS, "", 0, httpOnly = false)

    fun createCookie(
        request: HttpServletRequest,
        name: DomainCookie,
        value: String,
        maxAgeMillis: Long,
        httpOnly: Boolean = true,
    ): ResponseCookie {
        val serverName = request.serverName
        val isLocal = isLocalhost(serverName)
        val resolvedCookieName = resolveCookieName(name)

        val builder =
            ResponseCookie
                .from(resolvedCookieName, value)
                .httpOnly(httpOnly)
                .path("/")
                .maxAge(maxAgeMillis / 1000)
                .secure(!isLocal && cookieProperties.secure)
                .sameSite(if (isLocal) "Lax" else "None")

        val cookieDomain = resolveCookieDomain(serverName, isLocal)
        if (cookieDomain != null) {
            builder.domain(cookieDomain)
        }

        return builder.build()
    }

    fun resolveCookieName(name: DomainCookie): String {
        val prefix = cookieProperties.namePrefix.trim()
        if (prefix.isBlank()) return name.value
        val baseName = name.value.replaceFirstChar { it.uppercase() }
        return "$prefix$baseName"
    }

    private fun resolveCookieDomain(
        serverName: String,
        isLocal: Boolean,
    ): String? {
        if (isLocal) return null
        return cookieProperties.domain
            ?.trim()
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
    }

    private fun isLocalhost(host: String): Boolean {
        val normalizedHost = host.lowercase().substringBefore(":")
        return normalizedHost == "localhost" ||
            normalizedHost.endsWith(".localhost") ||
            normalizedHost == "127.0.0.1" ||
            normalizedHost == "::1"
    }
}
