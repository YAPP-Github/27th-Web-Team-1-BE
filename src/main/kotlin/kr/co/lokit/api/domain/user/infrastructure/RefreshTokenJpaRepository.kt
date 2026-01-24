package kr.co.lokit.api.domain.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, Long> {
    fun findByToken(token: String): RefreshTokenEntity?

    fun deleteByUser(user: UserEntity)
}
