package kr.co.lokit.api.domain.user.domain

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.constant.UserRole
import java.time.LocalDateTime

data class User(
    val id: Long = 0,
    val email: String,
    val name: String,
    val role: UserRole = UserRole.USER,
    var profileImageUrl: String? = null,
    val status: AccountStatus = AccountStatus.ACTIVE,
    val withdrawnAt: LocalDateTime? = null,
)
