package kr.co.lokit.api.domain.user.dto

import kr.co.lokit.api.domain.user.domain.User

data class LoginRequest(
    val email: String,
    val name: String,
) {
    fun toDomain(): User =
        User(
            email = email,
            name = name,
        )
}

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class JwtTokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
