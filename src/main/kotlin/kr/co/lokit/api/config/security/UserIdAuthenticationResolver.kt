package kr.co.lokit.api.config.security

import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
@Profile("local", "dev")
@Order(2)
class UserIdAuthenticationResolver(
    private val userDetailsService: CustomUserDetailsService,
) : AuthenticationResolver {
    override fun support(credentials: String): Boolean = credentials.toLongOrNull() != null

    override fun authenticate(credentials: String): UsernamePasswordAuthenticationToken {
        val userId = credentials.toLong()
        val userDetails = userDetailsService.loadUserById(userId)

        return UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.authorities,
        )
    }
}