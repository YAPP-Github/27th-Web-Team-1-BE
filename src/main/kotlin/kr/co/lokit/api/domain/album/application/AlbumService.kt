package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.album.mapping.toEntity
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
    fun create(albumRequest: AlbumRequest): IdResponse {
        val album = albumRequest.toEntity()
        val savedAlbum = albumRepository.save(album)

        return savedAlbum.id.toIdResponse()
    }
}
