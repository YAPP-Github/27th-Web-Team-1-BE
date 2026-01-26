package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import kr.co.lokit.api.domain.user.mapping.toDomain
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun login(user: User): User =
        userRepository.findByEmail(user.email) ?: userRepository.save(user)

    @Transactional
    fun refresh(refreshToken: String): JwtTokenResponse {
        val refreshTokenEntity =
            refreshTokenJpaRepository.findByToken(refreshToken)
                ?: throw BusinessException.InvalidRefreshTokenException()

        if (refreshTokenEntity.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenJpaRepository.delete(refreshTokenEntity)
            throw BusinessException.InvalidRefreshTokenException("만료된 리프레시 토큰입니다")
        }

        val user = refreshTokenEntity.user.toDomain()
        val accessToken = jwtTokenProvider.generateAccessToken(user)

        return JwtTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun generateTokensAndSave(user: User): JwtTokenResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = jwtTokenProvider.generateRefreshToken()

        val userEntity =
            userJpaRepository.findById(user.id).orElseThrow {
                BusinessException.UserNotFoundException()
            }

        refreshTokenJpaRepository.deleteByUser(userEntity)

        val expiresAt =
            LocalDateTime.now().plusSeconds(
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
