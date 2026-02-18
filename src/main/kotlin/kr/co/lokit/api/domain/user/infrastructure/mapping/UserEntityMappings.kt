package kr.co.lokit.api.domain.user.infrastructure.mapping

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun UserEntity.toDomain(): User =
    User(
        id = nonNullId(),
        email = email,
        name = name,
        role = role,
        profileImageUrl = profileImageUrl,
        status = status,
        withdrawnAt = withdrawnAt,
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        email = email,
        name = name,
        role = role,
        profileImageUrl = profileImageUrl,
        status = status,
        withdrawnAt = withdrawnAt,
    )
