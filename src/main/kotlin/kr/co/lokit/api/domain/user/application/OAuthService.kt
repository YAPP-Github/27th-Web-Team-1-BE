package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.concurrency.LockManager
import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.evictKey
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.AccountRecoveryPolicy
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
                    errors = errorDetailsOf(ErrorField.PROVIDER_ID to userInfo.providerId),
                )

        val name = userInfo.name ?: "${provider.name} 사용자"
        val loginResult =
            lockManager.withLock(key = User.emailLockKey(email), operation = {
                val loadedUser = userRepository.findByEmail(email, name)
                val updatedUser = userRepository.update(loadedUser.copy(profileImageUrl = userInfo.profileImageUrl))
                val recovered = reactivateIfNeeded(updatedUser)
                createCoupleUseCase.createIfNone(Couple(name = Couple.DEFAULT_COUPLE_NAME), loadedUser.id)
                LoginResult(user = updatedUser.recoveredIf(recovered), recovered = recovered)
            })

        if (loginResult.recovered) {
            cacheManager.evictKey(CacheRegion.USER_DETAILS, loginResult.user.email)
            cacheManager.evictKey(CacheRegion.USER_COUPLE, loginResult.user.id)
        }

        loginResult.user.profileImageUrl = userInfo.profileImageUrl
        return generateTokens(loginResult.user)
    }

    private fun reactivateIfNeeded(user: User): Boolean {
        val recoverable = AccountRecoveryPolicy.ensureRecoverable(user)
        if (recoverable) {
            userRepository.reactivate(user.id)
        }
        return recoverable
    }

    private fun User.recoveredIf(recovered: Boolean): User =
        if (recovered) {
            copy(status = AccountStatus.ACTIVE, withdrawnAt = null)
        } else {
            this
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

    private data class LoginResult(
        val user: User,
        val recovered: Boolean,
    )
}
