package kr.co.lokit.api.domain.user.mapping

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun UserEntity.toDomain(): User =
    User(
        id = id,
        email = username,
        name = getName(),
        role = UserRole.valueOf(role.name),
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        email = email,
        name = name,
        role = UserRole.valueOf(role.name),
    )

fun JwtTokenResponse.toJwtTokenResponse(): JwtTokenResponse =
    JwtTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
    )
