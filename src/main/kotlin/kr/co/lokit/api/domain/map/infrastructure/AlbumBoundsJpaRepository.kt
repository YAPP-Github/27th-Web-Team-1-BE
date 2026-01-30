package kr.co.lokit.api.domain.map.infrastructure

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface AlbumBoundsJpaRepository : JpaRepository<AlbumBoundsEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByStandardId(standardId: Long): AlbumBoundsEntity?

    @Query("select ab from album_bounds ab where ab.standardId = :standardId")
    fun findByStandardIdForRead(standardId: Long): AlbumBoundsEntity?
}
