package kr.co.lokit.api.config.security
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails =
        userRepository
            .findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")

    fun loadUserById(userId: Long): UserDetails =
        userRepository
            .findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found: $userId") }
}
