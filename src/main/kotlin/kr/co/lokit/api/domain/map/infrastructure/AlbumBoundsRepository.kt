package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.domain.map.domain.AlbumBounds

interface AlbumBoundsRepository {
    fun save(bounds: AlbumBounds): AlbumBounds
    fun findByAlbumIdOrNull(albumId: Long): AlbumBounds?
    fun apply(bounds: AlbumBounds): AlbumBounds
}
