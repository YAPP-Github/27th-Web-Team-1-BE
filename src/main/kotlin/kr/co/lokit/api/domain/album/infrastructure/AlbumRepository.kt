package kr.co.lokit.api.domain.album.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface AlbumRepository : JpaRepository<AlbumEntity, Long> {
}
