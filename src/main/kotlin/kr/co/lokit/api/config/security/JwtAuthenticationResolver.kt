package kr.co.lokit.api.config.security

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
@Order(2)
class JwtAuthenticationResolver(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: CustomUserDetailsService,
) : AuthenticationResolver {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun support(credentials: String): Boolean = jwtTokenProvider.canParse(credentials)

    override fun authenticate(credentials: String): UsernamePasswordAuthenticationToken? =
        try {
            val username = jwtTokenProvider.getUsernameFromToken(credentials)
            logger.debug("JWT parsed, username={}", username)
            val userDetails = userDetailsService.loadUserByUsername(username)
            logger.debug("User loaded, username={}", userDetails.username)

            UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.authorities,
            )
        } catch (e: Exception) {
            logger.error("JWT authentication failed: {}", e.message)
            null
        }
}
