package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
    fun create(albumRequest: AlbumRequest): IdResponse {
        val album = albumRequest.toDomain()
        val savedAlbum = albumRepository.save(album.toEntity())

        return IdResponse.from(savedAlbum)
    }
}
