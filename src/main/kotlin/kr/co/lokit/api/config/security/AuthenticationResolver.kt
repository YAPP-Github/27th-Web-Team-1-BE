package kr.co.lokit.api.config.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

interface AuthenticationResolver {
    fun support(credentials: String): Boolean

    fun authenticate(credentials: String): UsernamePasswordAuthenticationToken
}
