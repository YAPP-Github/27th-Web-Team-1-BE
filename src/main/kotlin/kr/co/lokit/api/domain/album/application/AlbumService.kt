package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumService(
    private val albumRepository: AlbumRepository,
) {
    @Transactional
    fun create(album: Album, userId: Long): Album =
        albumRepository.save(album, userId)

    @Transactional(readOnly = true)
    fun getSelectableAlbums(userId: Long): List<Album> =
        albumRepository.findAllByUserId(userId)

    @Transactional
    fun updateTitle(id: Long, title: String): Album =
        albumRepository.applyTitle(id, title)

    @Transactional
    fun delete(id: Long) =
        albumRepository.deleteById(id)
}
