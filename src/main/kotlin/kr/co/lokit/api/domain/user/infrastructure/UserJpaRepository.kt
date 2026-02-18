package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.common.constant.AccountStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import kr.co.lokit.api.common.concurrency.LockPolicy
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?

    fun findByStatusAndWithdrawnAtBefore(
        status: AccountStatus,
        cutoff: LocalDateTime,
    ): List<UserEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LockPolicy.DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT)])
    @Query("select u from Users u where u.id in :ids order by u.id asc")
    fun findAllByIdInForUpdate(ids: List<Long>): List<UserEntity>

    @Query(value = "select pg_advisory_xact_lock(hashtext(:email))", nativeQuery = true)
    fun lockWithEmail(email: String)
}
