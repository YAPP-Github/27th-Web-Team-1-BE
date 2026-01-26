package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.mapping.toDomain
import kr.co.lokit.api.domain.user.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        val entity = user.toEntity()
        return userJpaRepository.save(entity).toDomain()
    }

    override fun findById(id: Long): User? = userJpaRepository.findByIdOrNull(id)?.toDomain()

    override fun findByEmail(email: String): User? = userJpaRepository.findByEmail(email)?.toDomain()
}
