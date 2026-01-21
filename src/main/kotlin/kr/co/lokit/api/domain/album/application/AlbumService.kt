package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.album.mapping.toEntity
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
    fun create(album: Album): IdResponse {
        val savedAlbum = albumRepository.save(album.toEntity())

        return IdResponse.from(savedAlbum)
    }
}
