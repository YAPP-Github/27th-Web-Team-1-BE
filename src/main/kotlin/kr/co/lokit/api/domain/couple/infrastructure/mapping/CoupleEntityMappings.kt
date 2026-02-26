package kr.co.lokit.api.domain.couple.infrastructure.mapping

import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.InviteCode
import kr.co.lokit.api.domain.couple.domain.InviteIssuer
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.couple.infrastructure.InviteCodeEntity

fun Couple.toEntity(): CoupleEntity =
    CoupleEntity(
        name = name,
        status = status,
        disconnectedAt = disconnectedAt,
        disconnectedByUserId = disconnectedByUserId,
        firstMetDate = firstMetDate,
    )

fun CoupleEntity.toDomain(): Couple =
    Couple(
        id = nonNullId(),
        name = name,
        userIds = coupleUsers.map { it.user.nonNullId() },
        status = status,
        disconnectedAt = disconnectedAt,
        disconnectedByUserId = disconnectedByUserId,
        firstMetDate = firstMetDate,
    )

fun InviteCodeEntity.toDomain(): InviteCode =
    InviteCode(
        id = nonNullId(),
        code = code,
        createdBy =
            InviteIssuer(
                userId = createdBy.nonNullId(),
                name = createdBy.name,
                profileImageUrl = createdBy.profileImageUrl,
            ),
        status = status,
        expiresAt = expiresAt,
    )
