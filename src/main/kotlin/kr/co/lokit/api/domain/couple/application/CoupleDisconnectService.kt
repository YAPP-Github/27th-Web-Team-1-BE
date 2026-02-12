package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
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
        val couple = coupleRepository.findByUserId(userId)
            ?: throw BusinessException.CoupleNotFoundException(
                errors = mapOf("userId" to userId.toString()),
            )

        if (couple.status == CoupleStatus.DISCONNECTED) {
            throw BusinessException.CoupleAlreadyDisconnectedException(
                errors = mapOf("coupleId" to couple.id.toString()),
            )
        }

        // 1. 커플 상태를 DISCONNECTED로 변경
        coupleRepository.disconnect(couple.id, userId)

        // 2. 연결 끊기를 실행한 사용자의 CoupleUser 삭제
        coupleRepository.removeCoupleUser(userId)

        // 3. 캐시 무효화
        cacheManager.getCache("userCouple")?.evict(userId)
        couple.userIds.filter { it != userId }.forEach { partnerId ->
            cacheManager.getCache("userCouple")?.evict(partnerId)
        }
    }
}
