package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.domain.map.domain.BoundsIdType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AlbumBoundsJpaRepository : JpaRepository<AlbumBoundsEntity, Long> {
    @Query("select ab from album_bounds ab where ab.standardId = :standardId and ab.idType = :idType")
    fun findByStandardIdAndIdType(standardId: Long, idType: BoundsIdType): AlbumBoundsEntity?

    fun deleteAllByStandardIdIn(standardIds: List<Long>)
}
