package kr.co.lokit.api.domain.map.application.port

import kr.co.lokit.api.domain.map.domain.AlbumBounds

interface AlbumBoundsRepositoryPort {
    fun save(bounds: AlbumBounds): AlbumBounds
    fun findByAlbumIdOrNull(albumId: Long): AlbumBounds?
    fun apply(bounds: AlbumBounds): AlbumBounds
}

