package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
) : CreateCoupleUseCase, JoinCoupleUseCase {

    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = ["userCouple"], key = "#userId")
    override fun createIfNone(couple: Couple, userId: Long): Couple =
        coupleRepository.findByUserId(userId) ?: coupleRepository.saveWithUser(couple, userId)

    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = ["userCouple"], key = "#userId")
    override fun joinByInviteCode(inviteCode: String, userId: Long): Couple {
        val couple = coupleRepository.findByInviteCode(inviteCode)
            ?: throw entityNotFound<Couple>("inviteCode", inviteCode)
        return coupleRepository.addUser(couple.id, userId)
    }
}
