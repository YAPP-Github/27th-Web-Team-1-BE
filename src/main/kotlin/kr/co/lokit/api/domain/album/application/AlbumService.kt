package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
    fun create(album: Album): Album {
        return albumRepository.save(album)
    }

    fun getSelectableAlbums(userId: Long): List<Album> {
        return albumRepository.findAllByUserId(userId)
    }
}
