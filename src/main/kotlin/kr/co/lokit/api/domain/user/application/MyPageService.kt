package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.application.port.`in`.UpdateMyPageUseCase
import kr.co.lokit.api.domain.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyPageService(
    private val userRepository: UserRepositoryPort,
) : UpdateMyPageUseCase {
    @Transactional
    override fun updateNickname(
        userId: Long,
        nickname: String,
    ): User {
        val user = userRepository.findById(userId) ?: throw entityNotFound<User>(userId)
        return userRepository.update(user.copy(name = nickname))
    }

    @Transactional
    override fun updateProfileImage(
        userId: Long,
        profileImageUrl: String,
    ): User {
        val user = userRepository.findById(userId) ?: throw entityNotFound<User>(userId)
        return userRepository.update(user.copy(profileImageUrl = profileImageUrl))
    }
}
