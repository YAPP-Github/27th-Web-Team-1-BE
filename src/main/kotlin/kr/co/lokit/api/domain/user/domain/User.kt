package kr.co.lokit.api.domain.user.domain

import kr.co.lokit.api.common.constant.UserRole

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val role: UserRole = UserRole.USER,
    val profileImageUrl: String? = null,
)
