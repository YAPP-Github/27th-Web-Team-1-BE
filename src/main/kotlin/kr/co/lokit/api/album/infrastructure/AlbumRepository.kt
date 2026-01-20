package kr.co.lokit.api.album.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface AlbumRepository : JpaRepository<AlbumEntity, Long> {
}
