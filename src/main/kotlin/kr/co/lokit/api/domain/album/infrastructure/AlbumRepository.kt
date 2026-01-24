package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.domain.album.domain.Album

interface AlbumRepository {
    fun save(album: Album): Album
    fun findAllByUserId(userId: Long): List<Album>
}
