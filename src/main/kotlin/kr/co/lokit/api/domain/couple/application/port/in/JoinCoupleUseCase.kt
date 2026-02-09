package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.domain.Couple

interface JoinCoupleUseCase {
    fun joinByInviteCode(
        inviteCode: String,
        userId: Long,
    ): Couple

    fun getInviteCode(userId: Long): String
}
