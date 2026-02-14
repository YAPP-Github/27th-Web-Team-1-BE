package kr.co.lokit.api.domain.couple.domain

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.common.util.InviteCodeGenerator
import java.time.LocalDateTime

data class Couple(
    val id: Long = 0,
    val name: String,
    var inviteCode: String = InviteCodeGenerator.generate(),
    val userIds: List<Long> = emptyList(),
    val status: CoupleStatus = CoupleStatus.CONNECTED,
    val disconnectedAt: LocalDateTime? = null,
    val disconnectedByUserId: Long? = null,
) {
    init {
        require(userIds.size <= MAX_MEMBERS)
        require(inviteCode.length == INVITE_CODE_LENGTH)
    }

    fun isFull(): Boolean = userIds.size >= MAX_MEMBERS

    fun isDisconnected(): Boolean = status == CoupleStatus.DISCONNECTED

    fun isReconnectWindowExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        disconnectedAt
            ?.plusDays(GracePeriodPolicy.RECONNECT_DAYS)
            ?.isBefore(now)
            ?: true

    fun hasRemainingMemberForReconnect(): Boolean = userIds.isNotEmpty()

    fun deIdentifiedUserId(): Long? = disconnectedByUserId.takeIf { status.isDisconnectedOrExpired }

    companion object {
        const val DEFAULT_COUPLE_NAME = "default"
        const val MAX_MEMBERS = 2
        const val INVITE_CODE_LENGTH = 8
    }
}

enum class CoupleReconnectRejectReason(
    val code: String,
) {
    NO_REMAINING_MEMBER("no_remaining_member"),
}
