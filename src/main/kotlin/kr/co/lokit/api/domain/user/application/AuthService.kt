package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.mapping.toDomain
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
class AuthService(
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val transactionTemplate: TransactionTemplate,
) {
    @Transactional
    fun refreshIfValid(refreshToken: String): JwtTokenResponse? {
        val refreshTokenEntity = refreshTokenJpaRepository.findByToken(refreshToken) ?: return null

        if (refreshTokenEntity.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenJpaRepository.delete(refreshTokenEntity)
            return null
        }

        val user = refreshTokenEntity.user.toDomain()
        return generateTokensAndSave(user)
    }

    private fun generateTokensAndSave(user: User): JwtTokenResponse {
        val (accessTokenFuture, refreshTokenFuture, userEntityFuture) =
            StructuredConcurrency.run { scope ->
                Triple(
                    scope.fork { jwtTokenProvider.generateAccessToken(user) },
                    scope.fork { jwtTokenProvider.generateRefreshToken() },
                    scope.fork {
                        transactionTemplate.execute {
                            userJpaRepository.findByIdOrNull(user.id)
                                ?: throw BusinessException.UserNotFoundException(
                                    errors = mapOf("userId" to user.id.toString()),
                                )
                        }!!
                    },
                )
            }

        val accessToken = accessTokenFuture.get()
        val refreshToken = refreshTokenFuture.get()
        val userEntity = userEntityFuture.get()

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
