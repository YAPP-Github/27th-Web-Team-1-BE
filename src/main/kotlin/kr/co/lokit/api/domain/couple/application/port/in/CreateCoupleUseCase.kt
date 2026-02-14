package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.domain.Couple

interface CreateCoupleUseCase {
    fun createIfNone(
        couple: Couple,
        userId: Long,
    ): Couple
}
