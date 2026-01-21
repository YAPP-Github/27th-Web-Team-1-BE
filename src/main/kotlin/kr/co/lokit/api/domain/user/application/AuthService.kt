package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.AuthResult
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun register(
        email: String,
        name: String,
    ): AuthResult {
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.UserAlreadyExistsException()
        }

        val user =
            User(
                email = email,
                name = name,
            )

        val userEntity = UserEntity.from(user)
        val savedEntity = userRepository.save(userEntity)
        val token = jwtTokenProvider.generateToken(savedEntity)

        return AuthResult(
            token = token,
            user = savedEntity.toDomain(),
        )
    }
}
