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
        val currentCouple = coupleRepository.findByUserId(userId)

        if (currentCouple?.isConnectedAndFull() == true) {
            return CoupleCookieStatus.COUPLED
        }

        if (currentCouple != null && currentCouple.status.isDisconnectedOrExpired) {
            return if (currentCouple.disconnectedByUserId == userId) {
                CoupleCookieStatus.DISCONNECTED_BY_ME
            } else {
                resolveDisconnectedByPartner(currentCouple)
            }
        }

        if (currentCouple != null) {
            return CoupleCookieStatus.NOT_COUPLED
        }

        val disconnectedByMe = coupleRepository.findByDisconnectedByUserId(userId)
        if (disconnectedByMe?.status?.isDisconnectedOrExpired == true) {
            return CoupleCookieStatus.DISCONNECTED_BY_ME
        }

        return CoupleCookieStatus.NOT_COUPLED
    }

    private fun resolveDisconnectedByPartner(couple: Couple): CoupleCookieStatus {
        if (couple.status == CoupleStatus.EXPIRED || couple.isReconnectWindowExpired()) {
            return CoupleCookieStatus.DISCONNECTED_EXPIRED
        }

        val disconnectedByUserId = couple.disconnectedByUserId
        if (disconnectedByUserId != null) {
            val partnerCouple = coupleRepository.findByUserId(disconnectedByUserId)
            if (partnerCouple != null && partnerCouple.id != couple.id && partnerCouple.isConnectedAndFull()) {
                return CoupleCookieStatus.DISCONNECTED_EXPIRED
            }
        }

        return CoupleCookieStatus.DISCONNECTED_BY_PARTNER
    }
}
