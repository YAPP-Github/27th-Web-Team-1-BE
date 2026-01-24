package kr.co.lokit.api.config.security

import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
@Order(1)
class JwtAuthenticationResolver(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: CustomUserDetailsService,
) : AuthenticationResolver {
    override fun support(credentials: String): Boolean = jwtTokenProvider.canParse(credentials)

    override fun authenticate(credentials: String): UsernamePasswordAuthenticationToken {
        val username = jwtTokenProvider.getUsernameFromToken(credentials)
        val userDetails = userDetailsService.loadUserByUsername(username)

        return UsernamePasswordAuthenticationToken.authenticated(
            userDetails,
            null,
            userDetails.authorities,
        )
    }
}
