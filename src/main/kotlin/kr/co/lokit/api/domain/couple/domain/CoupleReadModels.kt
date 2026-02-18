package kr.co.lokit.api.domain.couple.domain

import java.time.LocalDateTime

data class CoupleStatusReadModel(
    val isCoupled: Boolean,
    val partnerSummary: PartnerSummaryReadModel? = null,
)

data class PartnerSummaryReadModel(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

data class InviteCodeIssueReadModel(
    val inviteCode: String,
    val expiresAt: LocalDateTime,
)

data class InviteCodePreviewReadModel(
    val inviterUserId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
