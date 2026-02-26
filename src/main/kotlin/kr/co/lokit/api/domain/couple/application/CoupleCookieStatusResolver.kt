package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constants.CoupleCookieStatus
import kr.co.lokit.api.common.constants.CoupleStatus
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.domain.Couple
import org.springframework.stereotype.Component

@Component
class CoupleCookieStatusResolver(
    private val coupleRepository: CoupleRepositoryPort,
) {
    fun resolve(userId: Long): CoupleCookieStatus {
        val currentCouple = coupleRepository.findByUserIdFresh(userId)

        if (currentCouple?.isConnectedAndFull() == true) {
            return CoupleCookieStatus.COUPLED
        }

        if (currentCouple != null && currentCouple.status.isDisconnectedOrExpired) {
            return resolveDisconnectedStatus(currentCouple, userId)
        }

        val disconnectedByMe = coupleRepository.findByDisconnectedByUserId(userId)
        if (disconnectedByMe != null) {
            return resolveDisconnectedStatus(disconnectedByMe, userId)
        }

        return CoupleCookieStatus.NOT_COUPLED
    }

    private fun resolveDisconnectedStatus(
        couple: Couple,
        userId: Long,
    ): CoupleCookieStatus {
        if (couple.status == CoupleStatus.EXPIRED || couple.isReconnectWindowExpired()) {
            return CoupleCookieStatus.DISCONNECTED_EXPIRED
        }

        val disconnectedByUserId = couple.disconnectedByUserId ?: return CoupleCookieStatus.NOT_COUPLED

        return if (disconnectedByUserId == userId) {
            CoupleCookieStatus.DISCONNECTED_BY_ME
        } else {
            CoupleCookieStatus.DISCONNECTED_BY_PARTNER
        }
    }
}
