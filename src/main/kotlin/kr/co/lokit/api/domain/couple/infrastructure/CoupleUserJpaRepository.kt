package kr.co.lokit.api.domain.couple.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CoupleUserJpaRepository : JpaRepository<CoupleUserEntity, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CoupleUser cu WHERE cu.user.id = :userId")
    fun deleteByUserId(userId: Long)
}
