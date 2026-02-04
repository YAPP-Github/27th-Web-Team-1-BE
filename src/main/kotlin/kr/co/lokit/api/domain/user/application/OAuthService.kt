package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthClientRegistry
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OAuthService(
    private val oAuthClientRegistry: OAuthClientRegistry,
    private val userRepository: UserRepositoryPort,
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val createCoupleUseCase: CreateCoupleUseCase,
) {
    @Transactional
    fun login(provider: OAuthProvider, code: String): JwtTokenResponse {
        val client = oAuthClientRegistry.getClient(provider)
        val accessToken = client.getAccessToken(code)
        val userInfo = client.getUserInfo(accessToken)

        val email = userInfo.email
            ?: throw BusinessException.KakaoEmailNotProvidedException(
                message = "${provider.name} 계정에서 이메일 정보를 제공받지 못했습니다",
                errors = mapOf("providerId" to userInfo.providerId),
            )

        val name = userInfo.name ?: "${provider.name} 사용자"

        val user = userRepository.findByEmail(email)
            ?: registerUser(email, name)

        return generateTokens(user)
    }

    private fun registerUser(email: String, name: String): User {
        val existing = userRepository.findByEmail(email)
        if (existing != null) {
            return existing
        }

        val user = userRepository.save(User(email = email, name = name))
        createCoupleUseCase.createIfNone(Couple(name = "default"), user.id)
        return user
    }

    private fun generateTokens(user: User): JwtTokenResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = jwtTokenProvider.generateRefreshToken()

        val userEntity = userJpaRepository.findByEmail(user.email)
            ?: throw BusinessException.UserNotFoundException(
                errors = mapOf("email" to user.email),
            )

        refreshTokenJpaRepository.deleteByUser(userEntity)

        val expiresAt = LocalDateTime.now().plusSeconds(
            jwtTokenProvider.getRefreshTokenExpirationMillis() / 1000,
        )

        refreshTokenJpaRepository.save(
            RefreshTokenEntity(
                token = refreshToken,
                user = userEntity,
                expiresAt = expiresAt,
            ),
        )

        return JwtTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
