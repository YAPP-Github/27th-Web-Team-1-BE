package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.domain.map.domain.AlbumBounds

interface AlbumBoundsRepository {
    fun save(bounds: AlbumBounds): AlbumBounds
    fun findByAlbumId(albumId: Long): AlbumBounds?
    fun updateBounds(bounds: AlbumBounds): AlbumBounds
}
