package kr.co.lokit.api.domain.user.mapping

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.LoginRequest
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

fun LoginRequest.toDomain(): User =
    User(
        email = email,
        name = "user",
    )
