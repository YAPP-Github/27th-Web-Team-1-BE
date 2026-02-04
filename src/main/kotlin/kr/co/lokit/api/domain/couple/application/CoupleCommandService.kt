package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
) : CreateCoupleUseCase, JoinCoupleUseCase {

    @Transactional
    @CacheEvict(cacheNames = ["coupleMembership"], key = "#userId + ':' + #result.id", condition = "#result != null")
    override fun createIfNone(couple: Couple, userId: Long): Couple =
        coupleRepository.findByUserId(userId) ?: coupleRepository.saveWithUser(couple, userId)

    @Transactional
    @CacheEvict(cacheNames = ["coupleMembership"], key = "#userId + ':' + #result.id", condition = "#result != null")
    override fun joinByInviteCode(inviteCode: String, userId: Long): Couple {
        val couple = coupleRepository.findByInviteCode(inviteCode)
            ?: throw entityNotFound<Couple>("inviteCode", inviteCode)
        return coupleRepository.addUser(couple.id, userId)
    }
}
