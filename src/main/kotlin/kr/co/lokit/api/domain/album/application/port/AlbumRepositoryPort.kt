package kr.co.lokit.api.domain.album.application.port

import kr.co.lokit.api.domain.album.domain.Album

interface AlbumRepositoryPort {
    fun save(album: Album, userId: Long): Album
    fun findById(id: Long): Album?
    fun findAllByUserId(userId: Long): List<Album>
    fun findAllByIds(ids: List<Long>): List<Album>
    fun applyTitle(id: Long, title: String): Album
    fun deleteById(id: Long)
    fun findAllWithPhotos(): List<Album>
    fun findByIdWithPhotos(id: Long): List<Album>
    fun findByIdWithPhotos(id: Long, userId: Long?): List<Album>
    fun findDefaultByUserId(userId: Long): Album?
    fun existsByCoupleIdAndTitle(coupleId: Long, title: String): Boolean
    fun photoCountSumByUserId(userId: Long): Int
}
