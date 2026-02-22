package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleProfileImage
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoupleCommandService(
    private val coupleRepository: CoupleRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val coupleProfileImageUrlResolver: CoupleProfileImageUrlResolver,
) : CreateCoupleUseCase {
    @OptimisticRetry
    @Transactional
    @CachePut(cacheNames = [CacheNames.USER_COUPLE], key = "#userId")
    override fun createIfNone(
        couple: Couple,
        userId: Long,
    ): Couple =
        coupleRepository.findByUserId(userId) ?: run {
            val created = coupleRepository.saveWithUser(couple, userId)
            val lockImageUrl = coupleProfileImageUrlResolver.resolve(CoupleProfileImage.LOCK)
            userRepository.findById(userId)?.let { user ->
                if (user.profileImageUrl != lockImageUrl) {
                    userRepository.update(user.withProfileImage(lockImageUrl))
                }
            }
            created
        }
}
