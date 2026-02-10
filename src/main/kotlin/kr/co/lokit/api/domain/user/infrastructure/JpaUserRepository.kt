package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.mapping.toDomain
import kr.co.lokit.api.domain.user.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaUserRepository(
    private val userJpaRepository: UserJpaRepository,
) : UserRepositoryPort {
    override fun save(user: User): User {
        val entity = user.toEntity()
        return userJpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): User? = userJpaRepository.findByIdOrNull(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun lockWithEmail(email: String) {
        userJpaRepository.lockWithEmail(email)
    }

    @Transactional
    override fun findByEmail(
        email: String,
        name: String,
    ): User {
        val userEntity =
            userJpaRepository.findByEmail(email) ?: userJpaRepository.save(User(email = email, name = name).toEntity())
        return userEntity.toDomain()
    }

    @Transactional
    override fun apply(
        user: User,
        name: String,
        profileImageUrl: String?,
    ) {
        val updatedUser =
            findById(user.id)?.copy(
                name = name,
                profileImageUrl = profileImageUrl,
            ) ?: throw entityNotFound<UserEntity>(user.id)
        save(updatedUser)
    }
}
