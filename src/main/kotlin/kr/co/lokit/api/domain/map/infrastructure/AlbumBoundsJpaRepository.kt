package kr.co.lokit.api.domain.map.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AlbumBoundsJpaRepository : JpaRepository<AlbumBoundsEntity, Long> {
    @Query("select ab from album_bounds ab where ab.standardId = :standardId")
    fun findByStandardIdForRead(standardId: Long): AlbumBoundsEntity?
}
