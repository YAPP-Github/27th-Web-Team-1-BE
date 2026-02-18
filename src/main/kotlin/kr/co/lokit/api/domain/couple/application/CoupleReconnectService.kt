package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.CoupleReconnectRejectReason
import kr.co.lokit.api.domain.couple.domain.CoupleReconnectRejection
import kr.co.lokit.api.domain.couple.application.mapping.toCoupledStatusReadModel
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleReconnectService(
    private val coupleRepository: CoupleRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val cacheManager: CacheManager,
) : ReconnectCoupleUseCase {
    @OptimisticRetry
    @Transactional
    override fun reconnect(userId: Long): CoupleStatusReadModel {
        val targetCouple =
            coupleRepository.findByDisconnectedByUserId(userId)
                ?: throw entityNotFound<Couple>("disconnectedByUserId", userId.toString())
        ensureReconnectable(targetCouple)
        pruneIncompleteCoupleIfExists(userId)

        val reconnected = coupleRepository.reconnect(targetCouple.id, userId)

        cacheManager.evictUserCoupleCache(userId, *reconnected.userIds.filter { it != userId }.toLongArray())
        evictPermissionCaches()

        val partnerId = reconnected.partnerIdFor(userId) ?: throw BusinessException.UserNotFoundException()
        val partner = userRepository.findById(partnerId) ?: throw BusinessException.UserNotFoundException()
        return reconnected.toCoupledStatusReadModel(partner)
    }

    private fun evictPermissionCaches() = cacheManager.clearPermissionCaches()

    private fun ensureReconnectable(targetCouple: Couple) {
        when (targetCouple.reconnectRejectionReason()) {
            CoupleReconnectRejection.NOT_DISCONNECTED ->
                throw BusinessException.CoupleNotDisconnectedException(
                    errors =
                        errorDetailsOf(
                            ErrorField.COUPLE_ID to targetCouple.id,
                            ErrorField.STATUS to targetCouple.status.name,
                        ),
                )
            CoupleReconnectRejection.RECONNECT_WINDOW_EXPIRED ->
                throw BusinessException.CoupleReconnectExpiredException(
                    errors = errorDetailsOf(ErrorField.COUPLE_ID to targetCouple.id),
                )
            CoupleReconnectRejection.NO_REMAINING_MEMBER ->
                throw BusinessException.CoupleReconnectNotAllowedException(
                    errors =
                        errorDetailsOf(
                            ErrorField.COUPLE_ID to targetCouple.id,
                            ErrorField.REASON to CoupleReconnectRejectReason.NO_REMAINING_MEMBER.code,
                        ),
                )
            null -> Unit
        }
    }

    private fun pruneIncompleteCoupleIfExists(userId: Long) {
        val existingCouple = coupleRepository.findByUserId(userId) ?: return
        val fullCouple = coupleRepository.findById(existingCouple.id) ?: return

        if (fullCouple.isConnectedAndFull()) {
            throw BusinessException.CoupleAlreadyConnectedException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to existingCouple.id),
            )
        }
        coupleRepository.deleteById(existingCouple.id)
    }
}
