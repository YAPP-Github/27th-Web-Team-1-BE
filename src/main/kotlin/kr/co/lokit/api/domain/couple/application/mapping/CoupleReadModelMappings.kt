package kr.co.lokit.api.domain.couple.application.mapping

import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCode
import kr.co.lokit.api.domain.couple.domain.InviteCodeIssueReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodePreviewReadModel
import kr.co.lokit.api.domain.couple.domain.PartnerSummaryReadModel
import kr.co.lokit.api.domain.user.domain.User

fun User.toPartnerSummaryReadModel(): PartnerSummaryReadModel =
    PartnerSummaryReadModel(
        userId = id,
        nickname = name,
        profileImageUrl = profileImageUrl,
    )

fun Couple.toCoupledStatusReadModel(partner: User): CoupleStatusReadModel =
    CoupleStatusReadModel(
        isCoupled = true,
        partnerSummary = partner.toPartnerSummaryReadModel(),
    )

fun Couple.toUncoupledStatusReadModel(): CoupleStatusReadModel = CoupleStatusReadModel(isCoupled = false)

fun uncoupledStatusReadModel(): CoupleStatusReadModel = CoupleStatusReadModel(isCoupled = false)

fun InviteCode.toPreviewReadModel(profileImageUrl: String? = createdBy.profileImageUrl): InviteCodePreviewReadModel =
    InviteCodePreviewReadModel(
        inviterUserId = createdBy.userId,
        nickname = createdBy.name,
        profileImageUrl = profileImageUrl,
    )

fun InviteCode.toIssueReadModel(): InviteCodeIssueReadModel = InviteCodeIssueReadModel(inviteCode = code, expiresAt = expiresAt)
