package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
) : CreateCoupleUseCase {
    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = [CacheNames.USER_COUPLE], key = "#userId")
    override fun createIfNone(
        couple: Couple,
        userId: Long,
    ): Couple = coupleRepository.findByUserId(userId) ?: coupleRepository.saveWithUser(couple, userId)
}
