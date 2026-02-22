package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.CoupleDisconnectAction
import kr.co.lokit.api.domain.couple.domain.Couple
import org.slf4j.LoggerFactory
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleDisconnectService(
    private val coupleRepository: CoupleRepositoryPort,
    private val createCoupleUseCase: CreateCoupleUseCase,
    private val cacheManager: CacheManager,
) : DisconnectCoupleUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun disconnect(userId: Long) {
        val couple =
            coupleRepository.findByUserId(userId)
                ?: throw BusinessException.CoupleNotFoundException(
                    errors = errorDetailsOf(ErrorField.USER_ID to userId),
                )

        when (couple.disconnectActionFor(userId)) {
            CoupleDisconnectAction.ALREADY_DISCONNECTED_BY_REQUESTER -> {
                throw BusinessException.CoupleAlreadyDisconnectedException(
                    errors = errorDetailsOf(ErrorField.COUPLE_ID to couple.id),
                )
            }
            CoupleDisconnectAction.REMOVE_MEMBER_ONLY -> coupleRepository.removeCoupleUser(userId)
            CoupleDisconnectAction.DISCONNECT_AND_REMOVE -> {
                coupleRepository.disconnect(couple.id, userId)
                coupleRepository.removeCoupleUser(userId)
            }
        }
        cacheManager.evictUserCoupleCache(userId, *couple.userIds.filter { it != userId }.toLongArray())
        createCoupleUseCase.createIfNone(Couple(name = Couple.DEFAULT_COUPLE_NAME), userId)
        evictPermissionCaches()
        log.info("couple_unlinked userId={} coupleId={}", userId, couple.id)
    }

    private fun evictPermissionCaches() = cacheManager.clearPermissionCaches()
}
