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
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        logger.debug("Should Not Filter That Request URI: $uri")
        return false
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val credential = request.getHeader("Authorization")

            if (credential != null && SecurityContextHolder.getContext().authentication == null) {
                compositeAuthenticationResolver.authenticate(credential)?.let {
                    it.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = it
                }
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}")
        }
        filterChain.doFilter(request, response)
    }
}
