package kr.co.lokit.api.domain.user.dto

import kr.co.lokit.api.domain.user.domain.User

data class RegisterRequest(
    val email: String,
    val name: String,
) {
    fun toDomain(): User =
        User(
            email = email,
            name = name,
        )
}

data class AuthResponse(
    val token: String,
    val email: String,
    val name: String,
)

data class AuthResult(
    val token: String,
    val user: User,
)
