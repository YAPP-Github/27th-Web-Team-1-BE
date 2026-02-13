package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.common.concurrency.LockManager
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthClientRegistry
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OAuthService(
    private val oAuthClientRegistry: OAuthClientRegistry,
    private val userRepository: UserRepositoryPort,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val jwtTokenProvider: JwtTokenProvider,
    private val createCoupleUseCase: CreateCoupleUseCase,
    private val lockManager: LockManager,
    private val cacheManager: CacheManager,
) {
    @Transactional
    fun login(
        provider: OAuthProvider,
        code: String,
    ): JwtTokenResponse {
        val client = oAuthClientRegistry.getClient(provider)
        val accessToken = client.getAccessToken(code)
        val userInfo = client.getUserInfo(accessToken)

        val email =
            userInfo.email
                ?: throw BusinessException.KakaoEmailNotProvidedException(
                    message = "${provider.name} 계정에서 이메일 정보를 제공받지 못했습니다",
                    errors = mapOf("providerId" to userInfo.providerId),
                )

        val name = userInfo.name ?: "${provider.name} 사용자"
        var recovered = false
        val user =
            lockManager.withLock(key = "email:$email", operation = {
                val user =
                    userRepository.findByEmail(email, name)

                val updated = userRepository.apply(user.copy(profileImageUrl = userInfo.profileImageUrl))

                if (updated.status == AccountStatus.WITHDRAWN) {
                    val withdrawnAt = updated.withdrawnAt
                        ?: throw BusinessException.ForbiddenException(
                            message = "탈퇴 계정 정보가 유효하지 않습니다",
                            errors = mapOf("userId" to updated.id.toString()),
                        )
                    if (withdrawnAt.plusDays(GracePeriodPolicy.RECONNECT_DAYS).isBefore(LocalDateTime.now())) {
                        throw BusinessException.UserRecoveryExpiredException(
                            errors = mapOf(
                                "userId" to updated.id.toString(),
                                "withdrawnAt" to withdrawnAt.toString(),
                            ),
                        )
                    }
                    userRepository.reactivate(updated.id)
                    recovered = true
                }

                createCoupleUseCase.createIfNone(
                    Couple(name = "default"),
                    user.id,
                )
                if (recovered) {
                    updated.copy(status = AccountStatus.ACTIVE, withdrawnAt = null)
                } else {
                    updated
                }
            })

        if (recovered) {
            cacheManager.getCache("userDetails")?.evict(user.email)
            cacheManager.getCache("userCouple")?.evict(user.id)
        }

        user.profileImageUrl = userInfo.profileImageUrl
        return generateTokens(user)
    }

    private fun generateTokens(user: User): JwtTokenResponse {
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

        return JwtTokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
