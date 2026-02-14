package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleDisconnectService(
    private val coupleRepository: CoupleRepositoryPort,
    private val cacheManager: CacheManager,
) : DisconnectCoupleUseCase {
    @Transactional
    override fun disconnect(userId: Long) {
        val couple =
            coupleRepository.findByUserId(userId)
                ?: throw BusinessException.CoupleNotFoundException(
                    errors = errorDetailsOf(ErrorField.USER_ID to userId),
                )

        if (couple.status.isDisconnectedOrExpired) {
            if (couple.disconnectedByUserId == userId) {
                throw BusinessException.CoupleAlreadyDisconnectedException(
                    errors = errorDetailsOf(ErrorField.COUPLE_ID to couple.id),
                )
            }

            coupleRepository.removeCoupleUser(userId)
            cacheManager.evictUserCoupleCache(userId, *couple.userIds.filter { it != userId }.toLongArray())
            evictPermissionCaches()
            return
        }

        // 1. 커플 상태를 DISCONNECTED로 변경
        coupleRepository.disconnect(couple.id, userId)

        // 2. 연결 끊기를 실행한 사용자의 CoupleUser 삭제
        coupleRepository.removeCoupleUser(userId)

        // 3. 캐시 무효화
        cacheManager.evictUserCoupleCache(userId, *couple.userIds.filter { it != userId }.toLongArray())
        evictPermissionCaches()
    }

    private fun evictPermissionCaches() = cacheManager.clearPermissionCaches()
}
