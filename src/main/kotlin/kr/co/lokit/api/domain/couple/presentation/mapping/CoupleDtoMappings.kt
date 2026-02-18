package kr.co.lokit.api.domain.couple.presentation.mapping

import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodeIssueReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodePreviewReadModel
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.PartnerSummaryResponse

fun CoupleStatusReadModel.toResponse(): CoupleStatusResponse =
    CoupleStatusResponse(
        isCoupled = isCoupled,
        partnerSummary = partnerSummary?.toResponse(),
    )

fun InviteCodeIssueReadModel.toResponse(): InviteCodeResponse =
    InviteCodeResponse(inviteCode = inviteCode, expiresAt = expiresAt)

fun InviteCodePreviewReadModel.toResponse(): InviteCodePreviewResponse =
    InviteCodePreviewResponse(
        inviterUserId = inviterUserId,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
    )

private fun kr.co.lokit.api.domain.couple.domain.PartnerSummaryReadModel.toResponse(): PartnerSummaryResponse =
    PartnerSummaryResponse(
        userId = userId,
        nickname = nickname,
        profileImageUrl = profileImageUrl,
    )
