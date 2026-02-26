package kr.co.lokit.api.domain.couple.domain

import kr.co.lokit.api.common.constants.CoupleStatus
import kr.co.lokit.api.common.constants.GracePeriodPolicy
import java.time.LocalDate
import java.time.LocalDateTime

data class Couple(
    val id: Long = 0,
    val name: String,
    val userIds: List<Long> = emptyList(),
    val status: CoupleStatus = CoupleStatus.CONNECTED,
    val disconnectedAt: LocalDateTime? = null,
    val disconnectedByUserId: Long? = null,
    val firstMetDate: LocalDate? = null,
) {
    init {
        require(userIds.size <= MAX_MEMBERS)
    }

    fun isFull(): Boolean = userIds.size >= MAX_MEMBERS

    fun isDisconnected(): Boolean = status == CoupleStatus.DISCONNECTED

    fun isReconnectWindowExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        disconnectedAt
            ?.plusDays(GracePeriodPolicy.RECONNECT_DAYS)
            ?.isBefore(now)
            ?: true

    fun hasRemainingMemberForReconnect(): Boolean = userIds.isNotEmpty()

    fun partnerIdFor(userId: Long): Long? = userIds.firstOrNull { it != userId }

    fun deIdentifiedUserId(): Long? = disconnectedByUserId.takeIf { status.isDisconnectedOrExpired }

    fun isConnectedAndFull(): Boolean = status == CoupleStatus.CONNECTED && isFull()

    fun disconnectActionFor(userId: Long): CoupleDisconnectAction =
        when {
            !status.isDisconnectedOrExpired -> CoupleDisconnectAction.DISCONNECT_AND_REMOVE
            disconnectedByUserId == userId -> CoupleDisconnectAction.ALREADY_DISCONNECTED_BY_REQUESTER
            else -> CoupleDisconnectAction.REMOVE_MEMBER_ONLY
        }

    fun reconnectRejectionReason(now: LocalDateTime = LocalDateTime.now()): CoupleReconnectRejection? =
        when {
            status != CoupleStatus.DISCONNECTED -> CoupleReconnectRejection.NOT_DISCONNECTED
            disconnectedAt == null -> CoupleReconnectRejection.NOT_DISCONNECTED
            isReconnectWindowExpired(now) -> CoupleReconnectRejection.RECONNECT_WINDOW_EXPIRED
            !hasRemainingMemberForReconnect() -> CoupleReconnectRejection.NO_REMAINING_MEMBER
            else -> null
        }

    companion object {
        const val DEFAULT_COUPLE_NAME = "default"
        const val MAX_MEMBERS = 2
    }
}

enum class CoupleDisconnectAction {
    DISCONNECT_AND_REMOVE,
    REMOVE_MEMBER_ONLY,
    ALREADY_DISCONNECTED_BY_REQUESTER,
}

enum class CoupleReconnectRejection {
    NOT_DISCONNECTED,
    RECONNECT_WINDOW_EXPIRED,
    NO_REMAINING_MEMBER,
}

enum class CoupleReconnectRejectReason(
    val code: String,
) {
    NO_REMAINING_MEMBER("no_remaining_member"),
}
