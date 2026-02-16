package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.dto.CoupleLinkResponse
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse

interface CoupleInviteUseCase {
    fun getMyStatus(userId: Long): CoupleStatusResponse

    fun generateInviteCode(
        userId: Long,
    ): InviteCodeResponse

    fun refreshInviteCode(
        userId: Long,
    ): InviteCodeResponse

    fun revokeInviteCode(
        userId: Long,
        inviteCode: String,
    )

    fun verifyInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): InviteCodePreviewResponse

    fun confirmInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): CoupleLinkResponse
}
