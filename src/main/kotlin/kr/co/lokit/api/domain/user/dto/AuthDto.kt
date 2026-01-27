package kr.co.lokit.api.domain.user.dto

import kr.co.lokit.api.domain.user.domain.User

data class LoginRequest(
    val email: String,
) {
    fun toDomain(): User =
        User(
            email = email,
            name = "user",
        )
}

data class LoginResponse(
    val userId: Long,
    val workspaceId: Long,
    val albumId: Long,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class JwtTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
