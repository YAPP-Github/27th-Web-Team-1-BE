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
) {
    fun withNickname(nickname: String): User = copy(name = nickname)

    fun withProfileImage(profileImageUrl: String?): User = copy(profileImageUrl = profileImageUrl)

    companion object {
        private const val EMAIL_LOCK_KEY_PREFIX = "email:"
        private const val DEFAULT_NICKNAME = "사용자"
        private const val MAX_NICKNAME_LENGTH = 10

        fun emailLockKey(email: String): String = "$EMAIL_LOCK_KEY_PREFIX$email"

        fun defaultNicknameFor(email: String): String =
            email
                .substringBefore("@")
                .trim()
                .ifBlank { DEFAULT_NICKNAME }
                .take(MAX_NICKNAME_LENGTH)
    }
}
