package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.mapping.toDomain
import kr.co.lokit.api.domain.user.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

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
    override fun update(user: User): User {
        val entity =
            userJpaRepository.findByIdOrNull(user.id)
                ?: throw entityNotFound<UserEntity>(user.id)
        entity.name = user.name
        entity.profileImageUrl = user.profileImageUrl
        return entity.toDomain()
    }

    @Transactional
    override fun withdraw(userId: Long) {
        val entity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<UserEntity>(userId)
        entity.status = AccountStatus.WITHDRAWN
        entity.withdrawnAt = LocalDateTime.now()
    }

    @Transactional
    override fun reactivate(userId: Long) {
        val entity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<UserEntity>(userId)
        entity.status = AccountStatus.ACTIVE
        entity.withdrawnAt = null
    }
}
