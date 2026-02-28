package kr.co.lokit.api.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.couple.application.CoupleCookieStatusResolver
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnBean(value = [CoupleCookieStatusResolver::class, CookieGenerator::class])
class CoupleStatusCookieSyncFilter(
    private val coupleCookieStatusResolver: CoupleCookieStatusResolver,
    private val cookieGenerator: CookieGenerator,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        filterChain.doFilter(request, response)

        if (response.isCommitted || shouldSkipSync(request)) {
            return
        }

        val userId = resolveAuthenticatedUserId() ?: return
        val status = coupleCookieStatusResolver.resolve(userId)

        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        response.setHeader("Pragma", "no-cache")
        response.setDateHeader("Expires", 0)
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieGenerator.createCoupleStatusCookie(request, status).toString(),
        )
    }

    private fun resolveAuthenticatedUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated) return null
        return (authentication.principal as? UserPrincipal)?.id
    }

    private fun shouldSkipSync(request: HttpServletRequest): Boolean {
        val method = request.method.uppercase()
        val path = request.requestURI.removePrefix(request.contextPath.orEmpty())

        if (method == "POST" && path == "/auth/logout") {
            return true
        }
        if (method == "DELETE" && path == "/users/me") {
            return true
        }
        return false
    }
}
