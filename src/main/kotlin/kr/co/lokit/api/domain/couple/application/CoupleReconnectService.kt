package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleReconnectRejectReason
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleReconnectService(
    private val coupleRepository: CoupleRepositoryPort,
    private val cacheManager: CacheManager,
) : ReconnectCoupleUseCase {
    @OptimisticRetry
    @Transactional
    override fun reconnect(userId: Long): Couple {
        val targetCouple =
            coupleRepository.findByDisconnectedByUserId(userId)
                ?: throw entityNotFound<Couple>("disconnectedByUserId", userId.toString())

        if (targetCouple.status != CoupleStatus.DISCONNECTED) {
            throw BusinessException.CoupleNotDisconnectedException(
                errors =
                    errorDetailsOf(
                        ErrorField.COUPLE_ID to targetCouple.id,
                        ErrorField.STATUS to targetCouple.status.name,
                    ),
            )
        }

        targetCouple.disconnectedAt
            ?: throw BusinessException.CoupleNotDisconnectedException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to targetCouple.id),
            )

        if (targetCouple.isReconnectWindowExpired()) {
            throw BusinessException.CoupleReconnectExpiredException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to targetCouple.id),
            )
        }

        if (!targetCouple.hasRemainingMemberForReconnect()) {
            throw BusinessException.CoupleReconnectNotAllowedException(
                errors =
                    errorDetailsOf(
                        ErrorField.COUPLE_ID to targetCouple.id,
                        ErrorField.REASON to CoupleReconnectRejectReason.NO_REMAINING_MEMBER.code,
                    ),
            )
        }

        val existingCouple = coupleRepository.findByUserId(userId)
        if (existingCouple != null) {
            val fullCouple = coupleRepository.findById(existingCouple.id)!!
            if (fullCouple.isFull()) {
                throw BusinessException.CoupleAlreadyConnectedException(
                    errors = errorDetailsOf(ErrorField.COUPLE_ID to existingCouple.id),
                )
            }
            coupleRepository.deleteById(existingCouple.id)
        }

        val reconnected = coupleRepository.reconnect(targetCouple.id, userId)

        cacheManager.evictUserCoupleCache(userId, *reconnected.userIds.filter { it != userId }.toLongArray())
        evictPermissionCaches()

        return reconnected
    }

    private fun evictPermissionCaches() = cacheManager.clearPermissionCaches()
}
