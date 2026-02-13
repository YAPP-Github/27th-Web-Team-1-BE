package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
    private val cacheManager: CacheManager,
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

        if (targetCouple.status == CoupleStatus.EXPIRED) {
            throw BusinessException.CoupleReconnectExpiredException(
                errors = mapOf("coupleId" to targetCouple.id.toString()),
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

        val joined =
            if (targetCouple.status == CoupleStatus.DISCONNECTED) {
                val disconnectedAt = targetCouple.disconnectedAt
                    ?: throw BusinessException.CoupleNotDisconnectedException(
                        errors = mapOf("coupleId" to targetCouple.id.toString()),
                    )
                if (isReconnectWindowExpired(disconnectedAt)) {
                    throw BusinessException.CoupleReconnectExpiredException(
                        errors = mapOf("coupleId" to targetCouple.id.toString()),
                    )
                }
                if (targetCouple.userIds.isEmpty()) {
                    throw BusinessException.CoupleReconnectNotAllowedException(
                        errors = mapOf(
                            "coupleId" to targetCouple.id.toString(),
                            "reason" to "no_remaining_member",
                        ),
                    )
                }
                coupleRepository.reconnect(targetCouple.id, userId)
            } else {
                coupleRepository.addUser(targetCouple.id, userId)
            }

        cacheManager.getCache("userCouple")?.evict(userId)
        joined.userIds.forEach { memberId ->
            cacheManager.getCache("userCouple")?.evict(memberId)
        }
        evictPermissionCaches()

        return joined
    }

    override fun getInviteCode(userId: Long): String =
        (coupleRepository.findByUserId(userId) ?: throw entityNotFound<Couple>(userId)).inviteCode

    private fun evictPermissionCaches() {
        cacheManager.getCache("album")?.clear()
        cacheManager.getCache("photo")?.clear()
        cacheManager.getCache("albumCouple")?.clear()
    }

    private fun isReconnectWindowExpired(disconnectedAt: LocalDateTime): Boolean =
        disconnectedAt.plusDays(GracePeriodPolicy.RECONNECT_DAYS).isBefore(LocalDateTime.now())
}
