package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictKey
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.application.port.`in`.WithdrawUseCase
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserWithdrawService(
    private val userRepository: UserRepositoryPort,
    private val coupleRepository: CoupleRepositoryPort,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val cacheManager: CacheManager,
) : WithdrawUseCase {
    @Transactional
    override fun withdraw(userId: Long) {
        val user =
            userRepository.findById(userId)
                ?: throw BusinessException.UserNotFoundException(
                    errors = errorDetailsOf(ErrorField.USER_ID to userId),
                )

        // 1. Refresh Token 전부 삭제
        refreshTokenRepository.deleteByUserId(userId)

        // 2. 회원 탈퇴는 반드시 연결 끊기 완료 후에만 허용
        val couple = coupleRepository.findByUserId(userId)
        if (couple != null) {
            throw BusinessException.UserDisconnectRequiredException(
                errors =
                    errorDetailsOf(
                        ErrorField.USER_ID to userId,
                        ErrorField.COUPLE_ID to couple.id,
                        ErrorField.COUPLE_STATUS to couple.status.name,
                    ),
            )
        }

        // 3. User status → WITHDRAWN, withdrawnAt 설정
        userRepository.withdraw(userId)

        // 4. 캐시 무효화
        cacheManager.evictKey(CacheRegion.USER_DETAILS, user.email)
        cacheManager.evictUserCoupleCache(userId)
        cacheManager.clearPermissionCaches()
    }
}
