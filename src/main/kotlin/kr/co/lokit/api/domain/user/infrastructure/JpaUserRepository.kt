package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.mapping.toDomain
import kr.co.lokit.api.domain.user.infrastructure.mapping.toEntity
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
    override fun findByEmail(email: String): User {
        val userEntity =
            userJpaRepository.findByEmail(email)
                ?: userJpaRepository.save(
                    User(
                        email = email,
                        name = User.defaultNicknameFor(email),
                    ).toEntity(),
                )
        return userEntity.toDomain()
    }

    @Transactional
    override fun update(user: User): User {
        val entity =
            userJpaRepository.findByIdOrNull(user.id)
                ?: throw entityNotFound<UserEntity>(user.id)
        entity.apply(user)
        return entity.toDomain()
    }

    @Transactional
    override fun lockByIds(ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }
        userJpaRepository.findAllByIdInForUpdate(ids.sorted())
    }

    @Transactional
    override fun withdraw(userId: Long) {
        val entity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<UserEntity>(userId)
        entity.markWithdrawn(LocalDateTime.now())
    }

    @Transactional
    override fun reactivate(userId: Long) {
        val entity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<UserEntity>(userId)
        entity.reactivate()
    }
}
