package kr.co.lokit.api.domain.couple.mapping

import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity

fun Couple.toEntity(): CoupleEntity =
    CoupleEntity(
        name = this.name,
        inviteCode = this.inviteCode,
        status = this.status,
        disconnectedAt = this.disconnectedAt,
        disconnectedByUserId = this.disconnectedByUserId,
    )

fun CoupleEntity.toDomain(): Couple =
    Couple(
        id = this.nonNullId(),
        name = this.name,
        inviteCode = this.inviteCode,
        userIds = this.coupleUsers.map { it.user.nonNullId() },
        status = this.status,
        disconnectedAt = this.disconnectedAt,
        disconnectedByUserId = this.disconnectedByUserId,
    )
