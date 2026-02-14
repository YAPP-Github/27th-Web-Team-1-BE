package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleReconnectRejectReason
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
    private val cacheManager: CacheManager,
) : CreateCoupleUseCase,
    JoinCoupleUseCase {
    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = [CacheNames.USER_COUPLE], key = "#userId")
    override fun createIfNone(
        couple: Couple,
        userId: Long,
    ): Couple = coupleRepository.findByUserId(userId) ?: coupleRepository.saveWithUser(couple, userId)

    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = [CacheNames.USER_COUPLE], key = "#userId")
    override fun joinByInviteCode(
        inviteCode: String,
        userId: Long,
    ): Couple {
        val targetCouple = resolveJoinTarget(inviteCode)
        if (targetCouple.status == CoupleStatus.EXPIRED) {
            throw BusinessException.CoupleReconnectExpiredException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to targetCouple.id),
            )
        }

        detachExistingCoupleIfJoinable(userId)
        val joined = joinTargetCouple(targetCouple, userId)

        cacheManager.evictUserCoupleCache(userId, *joined.userIds.toLongArray())
        evictPermissionCaches()

        return joined
    }

    override fun getInviteCode(userId: Long): String =
        (coupleRepository.findByUserId(userId) ?: throw entityNotFound<Couple>(userId)).inviteCode

    private fun resolveJoinTarget(inviteCode: String): Couple =
        coupleRepository.findByInviteCode(inviteCode)
            ?: throw entityNotFound<Couple>("inviteCode", inviteCode)

    private fun detachExistingCoupleIfJoinable(userId: Long) {
        val existingCouple = coupleRepository.findByUserId(userId) ?: return
        val fullCouple = coupleRepository.findById(existingCouple.id)!!
        if (fullCouple.isFull()) {
            throw BusinessException.CoupleAlreadyConnectedException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to existingCouple.id),
            )
        }
        coupleRepository.deleteById(existingCouple.id)
    }

    private fun joinTargetCouple(
        targetCouple: Couple,
        userId: Long,
    ): Couple {
        if (targetCouple.status != CoupleStatus.DISCONNECTED) {
            return coupleRepository.addUser(targetCouple.id, userId)
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
        return coupleRepository.reconnect(targetCouple.id, userId)
    }

    private fun evictPermissionCaches() = cacheManager.clearPermissionCaches()
}
