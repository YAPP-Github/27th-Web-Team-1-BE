package kr.co.lokit.api.domain.user.domain

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
