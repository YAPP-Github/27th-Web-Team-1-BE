package kr.co.lokit.api.domain.map.infrastructure

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface AlbumBoundsJpaRepository : JpaRepository<AlbumBoundsEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByStandardId(standardId: Long): AlbumBoundsEntity?
}
