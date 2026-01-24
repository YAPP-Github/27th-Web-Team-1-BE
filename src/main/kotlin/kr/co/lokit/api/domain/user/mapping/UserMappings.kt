package kr.co.lokit.api.domain.user.mapping

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.domain.UserRole
import kr.co.lokit.api.domain.user.dto.AuthResponse
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.Role
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
        role = Role.valueOf(role.name),
    )

fun JwtTokenResponse.toAuthResponse(): AuthResponse =
    AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        email = user.email,
        name = user.name,
    )
