package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.exception.BusinessException
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
) : CreateCoupleUseCase,
    JoinCoupleUseCase {
    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = ["userCouple"], key = "#userId")
    override fun createIfNone(
        couple: Couple,
        userId: Long,
    ): Couple = coupleRepository.findByUserId(userId) ?: coupleRepository.saveWithUser(couple, userId)

    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = ["userCouple"], key = "#userId")
    override fun joinByInviteCode(
        inviteCode: String,
        userId: Long,
    ): Couple {
        val targetCouple =
            coupleRepository.findByInviteCode(inviteCode)
                ?: throw entityNotFound<Couple>("inviteCode", inviteCode)

        val existingCouple = coupleRepository.findByUserId(userId)
        if (existingCouple != null) {
            val fullCouple = coupleRepository.findById(existingCouple.id)!!
            if (fullCouple.userIds.size >= 2) {
                throw BusinessException.CoupleAlreadyConnectedException(
                    errors = mapOf("coupleId" to existingCouple.id.toString()),
                )
            }
            coupleRepository.deleteById(existingCouple.id)
        }

        return coupleRepository.addUser(targetCouple.id, userId)
    }

    override fun getInviteCode(userId: Long): String =
        (coupleRepository.findByUserId(userId) ?: throw entityNotFound<Couple>(userId)).inviteCode
}
