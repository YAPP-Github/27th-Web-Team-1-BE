package kr.co.lokit.api.domain.map.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface AlbumBoundsJpaRepository : JpaRepository<AlbumBoundsEntity, Long> {
    fun findByAlbumId(albumId: Long): AlbumBoundsEntity?
}
