package kr.co.lokit.api.domain.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?

    @Query(value = "select pg_advisory_xact_lock(hashtext(:email))", nativeQuery = true)
    fun lockWithEmail(email: String)
}
