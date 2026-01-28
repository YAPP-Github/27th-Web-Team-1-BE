package kr.co.lokit.api.config.security

import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails =
        userJpaRepository
            .findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")

    fun loadUserById(userId: Long): UserDetails =
        userJpaRepository
            .findByIdOrNull(userId)
            ?: throw UsernameNotFoundException("User not found: $userId")
}
