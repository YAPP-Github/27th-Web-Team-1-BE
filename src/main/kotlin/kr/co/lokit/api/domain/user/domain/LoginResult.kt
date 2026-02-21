package kr.co.lokit.api.domain.user.domain

data class LoginResult(
    val userId: Long,
    val tokens: AuthTokens,
)
