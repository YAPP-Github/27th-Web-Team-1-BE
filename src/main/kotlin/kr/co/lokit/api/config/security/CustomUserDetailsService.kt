package kr.co.lokit.api.config.security

import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userJpaRepository: UserJpaRepository,
) : UserDetailsService {
    @Cacheable(cacheNames = [CacheNames.USER_DETAILS], key = "#username", sync = true)
    override fun loadUserByUsername(username: String): UserDetails =
        userJpaRepository
            .findByEmail(username)
            ?.let { UserPrincipal.from(it) }
            ?: throw UsernameNotFoundException("User not found: $username")
}
