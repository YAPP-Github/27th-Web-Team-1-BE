package kr.co.lokit.api.domain.couple.application.port.`in`

import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodeIssueReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodePreviewReadModel

interface CoupleInviteUseCase {
    fun getMyStatus(userId: Long): CoupleStatusReadModel

    fun generateInviteCode(userId: Long): InviteCodeIssueReadModel

    fun refreshInviteCode(userId: Long): InviteCodeIssueReadModel

    fun revokeInviteCode(
        userId: Long,
        inviteCode: String,
    )

    fun verifyInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): InviteCodePreviewReadModel

    fun confirmInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): CoupleStatusReadModel

    fun joinByInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): CoupleStatusReadModel
}
