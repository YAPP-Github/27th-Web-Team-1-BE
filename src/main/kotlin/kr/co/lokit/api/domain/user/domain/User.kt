package kr.co.lokit.api.domain.user.domain

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val role: UserRole = UserRole.USER,
)

enum class UserRole {
    USER,
    ADMIN,
}
