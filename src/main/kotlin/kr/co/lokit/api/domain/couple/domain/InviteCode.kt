package kr.co.lokit.api.domain.couple.domain

import java.time.LocalDateTime

data class InviteCode(
    val id: Long,
    val code: String,
    val createdBy: InviteIssuer,
    val status: InviteCodeStatus,
    val expiresAt: LocalDateTime,
) {
    fun isExpired(now: LocalDateTime): Boolean = expiresAt <= now

    fun isOwnedBy(userId: Long): Boolean = createdBy.userId == userId

    fun rejectionReason(now: LocalDateTime): InviteCodeRejectionReason? =
        when {
            isExpired(now) -> InviteCodeRejectionReason.EXPIRED
            status == InviteCodeStatus.USED -> InviteCodeRejectionReason.USED
            status == InviteCodeStatus.REVOKED -> InviteCodeRejectionReason.REVOKED
            status == InviteCodeStatus.EXPIRED -> InviteCodeRejectionReason.EXPIRED
            else -> null
        }
}

data class InviteIssuer(
    val userId: Long,
    val name: String,
    val profileImageUrl: String?,
)

enum class InviteCodeRejectionReason {
    EXPIRED,
    USED,
    REVOKED,
}
