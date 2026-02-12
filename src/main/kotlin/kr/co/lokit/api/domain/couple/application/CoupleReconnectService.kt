package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CoupleReconnectService(
    private val coupleRepository: CoupleRepositoryPort,
    private val cacheManager: CacheManager,
) : ReconnectCoupleUseCase {

    @OptimisticRetry
    @Transactional
    override fun reconnect(inviteCode: String, userId: Long): Couple {
        val targetCouple = coupleRepository.findByInviteCode(inviteCode)
            ?: throw entityNotFound<Couple>("inviteCode", inviteCode)

        if (targetCouple.status != CoupleStatus.DISCONNECTED) {
            throw BusinessException.CoupleNotDisconnectedException(
                errors = mapOf("coupleId" to targetCouple.id.toString(), "status" to targetCouple.status.name),
            )
        }

        val disconnectedAt = targetCouple.disconnectedAt
            ?: throw BusinessException.CoupleNotDisconnectedException(
                errors = mapOf("coupleId" to targetCouple.id.toString()),
            )

        if (disconnectedAt.plusDays(GracePeriodPolicy.RECONNECT_DAYS).isBefore(LocalDateTime.now())) {
            throw BusinessException.CoupleReconnectExpiredException(
                errors = mapOf("coupleId" to targetCouple.id.toString()),
            )
        }

        if (targetCouple.disconnectedByUserId != userId) {
            throw BusinessException.CoupleReconnectNotAllowedException(
                errors = mapOf("coupleId" to targetCouple.id.toString(), "userId" to userId.toString()),
            )
        }

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

        val reconnected = coupleRepository.reconnect(targetCouple.id, userId)

        cacheManager.getCache("userCouple")?.evict(userId)
        reconnected.userIds.filter { it != userId }.forEach { partnerId ->
            cacheManager.getCache("userCouple")?.evict(partnerId)
        }

        return reconnected
    }
}
