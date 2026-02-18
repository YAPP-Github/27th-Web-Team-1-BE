package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel

interface ReconnectCoupleUseCase {
    fun reconnect(userId: Long): CoupleStatusReadModel
}
