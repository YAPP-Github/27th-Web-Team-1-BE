package kr.co.lokit.api.domain.couple.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface CoupleUserJpaRepository : JpaRepository<CoupleUserEntity, Long> {
    @Modifying
    @Query("DELETE FROM CoupleUser cu WHERE cu.user.id = :userId")
    fun deleteByUserId(userId: Long)

    @Query("SELECT MAX(cu.createdAt) FROM CoupleUser cu WHERE cu.couple.id = :coupleId")
    fun findLatestJoinedAtByCoupleId(coupleId: Long): LocalDateTime?
}
