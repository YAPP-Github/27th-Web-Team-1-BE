package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.AuthTokens
import kr.co.lokit.api.domain.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepositoryPort,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    @Transactional
    fun refreshIfValid(refreshToken: String): AuthTokens? {
        val refreshTokenRecord = refreshTokenRepository.findByToken(refreshToken) ?: return null

        if (refreshTokenRecord.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(refreshToken)
            return null
        }

        val user =
            userRepository.findById(refreshTokenRecord.userId)
                ?: throw BusinessException.UserNotFoundException(
                    errors = errorDetailsOf(ErrorField.USER_ID to refreshTokenRecord.userId),
                )

        return generateTokensAndSave(user)
    }

    private fun generateTokensAndSave(user: User): AuthTokens {
        val accessToken = jwtTokenProvider.generateAccessToken(user)
        val refreshToken = jwtTokenProvider.generateRefreshToken()

        val expiresAt =
            LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMillis() / 1000,
            )

        refreshTokenRepository.replace(
            userId = user.id,
            token = refreshToken,
            expiresAt = expiresAt,
        )

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
