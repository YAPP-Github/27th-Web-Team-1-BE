package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.common.constant.AccountStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?

    fun findByStatusAndWithdrawnAtBefore(
        status: AccountStatus,
        cutoff: LocalDateTime,
    ): List<UserEntity>

    @Query(value = "select pg_advisory_xact_lock(hashtext(:email))", nativeQuery = true)
    fun lockWithEmail(email: String)
}
