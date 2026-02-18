package kr.co.lokit.api.domain.couple.infrastructure

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import kr.co.lokit.api.common.concurrency.LockPolicy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime

interface InviteCodeJpaRepository : JpaRepository<InviteCodeEntity, Long> {
    fun existsByCode(code: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LockPolicy.DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT)])
    @Query(
        """
        select i from InviteCode i
        join fetch i.createdBy
        where i.code = :code
        """,
    )
    fun findByCodeForUpdate(code: String): InviteCodeEntity?

    @Query(
        """
        select i from InviteCode i
        where i.code = :code
        """,
    )
    fun findByCode(code: String): InviteCodeEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LockPolicy.DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT)])
    @Query(
        """
        select i from InviteCode i
        where i.createdBy.id = :userId
          and i.status = 'UNUSED'
          and i.expiresAt > :now
        order by i.createdAt desc
        """,
    )
    fun findActiveUnusedByUserIdForUpdate(
        userId: Long,
        now: LocalDateTime,
    ): List<InviteCodeEntity>

    @Modifying
    @Query(
        """
        delete from InviteCode i
        where i.status = 'UNUSED'
          and i.expiresAt <= :now
        """,
    )
    fun hardDeleteExpiredUnusedCodes(now: LocalDateTime): Int

    @Modifying
    @Query(value = "delete from invite_code where id = :id", nativeQuery = true)
    fun hardDeleteById(id: Long): Int
}
