package kr.co.lokit.api.common.constants

enum class UserRole(
    val authority: String,
) {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
}
