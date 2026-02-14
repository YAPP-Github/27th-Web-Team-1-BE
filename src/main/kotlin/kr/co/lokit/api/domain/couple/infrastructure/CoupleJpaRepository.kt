package kr.co.lokit.api.domain.couple.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CoupleJpaRepository : JpaRepository<CoupleEntity, Long> {
    @Query(
        """
        select c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where c.inviteCode = :inviteCode
        """,
    )
    fun findByInviteCode(inviteCode: String): CoupleEntity?

    @Query(
        """
        select c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where c.id = :id
        """,
    )
    fun findByIdFetchUsers(id: Long): CoupleEntity?

    @Query(
        """
        select c from Couple c
        join fetch c.coupleUsers cu
        where cu.user.id = :userId
        """,
    )
    fun findByUserId(userId: Long): CoupleEntity?

    @Query(
        """
        select c from Couple c
        left join fetch c.coupleUsers cu
        left join fetch cu.user
        where c.disconnectedByUserId = :userId
        and c.status = 'DISCONNECTED'
        """,
    )
    fun findByDisconnectedByUserId(userId: Long): CoupleEntity?

    @Query("SELECT c FROM Couple c WHERE c.status = 'DISCONNECTED' AND c.disconnectedAt < :cutoff")
    fun findDisconnectedBefore(cutoff: LocalDateTime): List<CoupleEntity>

    @Query("SELECT c FROM Couple c WHERE c.status = 'EXPIRED' AND c.disconnectedAt < :cutoff")
    fun findExpiredBefore(cutoff: LocalDateTime): List<CoupleEntity>
}
