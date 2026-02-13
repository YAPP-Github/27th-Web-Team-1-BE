package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.domain.Couple

interface ReconnectCoupleUseCase {
    fun reconnect(userId: Long): Couple
}
