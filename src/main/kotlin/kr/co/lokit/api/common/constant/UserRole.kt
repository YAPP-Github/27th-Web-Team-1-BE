package kr.co.lokit.api.common.constant

enum class UserRole(
    val authority: String,
) {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
}
