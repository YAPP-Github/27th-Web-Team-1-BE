package kr.co.lokit.api.domain.user.mapping

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun UserEntity.toDomain(): User =
    User(
        id = nonNullId(),
        email = email,
        name = name,
        role = role,
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        email = email,
        name = name,
        role = role,
    )

fun JwtTokenResponse.toJwtTokenResponse(): JwtTokenResponse =
    JwtTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
    )
