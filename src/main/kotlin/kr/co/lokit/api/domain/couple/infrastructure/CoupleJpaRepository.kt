package kr.co.lokit.api.domain.couple.infrastructure

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import kr.co.lokit.api.common.concurrency.LockPolicy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.time.LocalDateTime

interface CoupleJpaRepository : JpaRepository<CoupleEntity, Long> {
    @Query(
        """
        select c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where c.id = :id
        """,
    )
    fun findByIdFetchUsers(id: Long): CoupleEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = LockPolicy.DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT)])
    @Query("select c from Couple c where c.id = :id")
    fun findByIdForUpdate(id: Long): CoupleEntity?

    @Query(
        """
        select distinct c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where exists (
            select 1
            from CoupleUser cu2
            where cu2.couple = c
            and cu2.user.id = :userId
        )
        """,
    )
    fun findByUserIdCandidates(userId: Long): List<CoupleEntity>

    @Query(
        """
        select c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where c.disconnectedByUserId = :userId
        and c.status = 'DISCONNECTED'
        """,
    )
    fun findByDisconnectedByUserIdCandidates(userId: Long): List<CoupleEntity>

    @Query("SELECT c FROM Couple c WHERE c.status = 'DISCONNECTED' AND c.disconnectedAt < :cutoff")
    fun findDisconnectedBefore(cutoff: LocalDateTime): List<CoupleEntity>

    @Query("SELECT c FROM Couple c WHERE c.status = 'EXPIRED' AND c.disconnectedAt < :cutoff")
    fun findExpiredBefore(cutoff: LocalDateTime): List<CoupleEntity>
}
