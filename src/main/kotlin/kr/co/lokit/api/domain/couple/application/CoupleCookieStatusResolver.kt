package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constants.CoupleCookieStatus
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
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
                CoupleCookieStatus.DISCONNECTED_BY_PARTNER
            }
        }

        val disconnectedByMe = coupleRepository.findByDisconnectedByUserId(userId)
        if (disconnectedByMe?.status?.isDisconnectedOrExpired == true) {
            return CoupleCookieStatus.DISCONNECTED_BY_ME
        }

        return CoupleCookieStatus.NOT_COUPLED
    }
}
