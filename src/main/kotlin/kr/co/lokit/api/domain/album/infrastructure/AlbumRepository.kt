package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.domain.album.domain.Album

interface AlbumRepository {
    fun save(album: Album, userId: Long): Album
    fun findById(id: Long): Album?
    fun findAllByUserId(userId: Long): List<Album>
    fun applyTitle(id: Long, title: String): Album
    fun deleteById(id: Long)
    fun findAllWithPhotos(): List<Album>
    fun findByIdWithPhotos(id: Long): List<Album>
}
