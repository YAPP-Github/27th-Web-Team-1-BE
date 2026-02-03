package kr.co.lokit.api.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AuthenticationFilter(
    private val compositeAuthenticationResolver: CompositeAuthenticationResolver,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val credential = getCredentialFromCookie(request) ?: request.getHeader("Authorization")

            if (credential != null && SecurityContextHolder.getContext().authentication == null) {
                compositeAuthenticationResolver.authenticate(credential)?.let { authentication ->
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                    val securityContext = SecurityContextHolder.createEmptyContext()
                    securityContext.authentication = authentication
                    SecurityContextHolder.setContext(securityContext)
                }
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun getCredentialFromCookie(request: HttpServletRequest): String? =
        request.cookies
            ?.find { it.name == "accessToken" }
            ?.value
            ?.takeIf { it.isNotBlank() && !it.contains(" ") }
}
