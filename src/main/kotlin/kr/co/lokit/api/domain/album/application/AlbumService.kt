package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
}
