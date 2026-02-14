package kr.co.lokit.api.domain.user.application.port.`in`

import kr.co.lokit.api.domain.user.domain.User

interface UpdateMyPageUseCase {
    fun updateNickname(
        userId: Long,
        nickname: String,
    ): User

    fun updateProfileImage(
        userId: Long,
        profileImageUrl: String,
    ): User
}
