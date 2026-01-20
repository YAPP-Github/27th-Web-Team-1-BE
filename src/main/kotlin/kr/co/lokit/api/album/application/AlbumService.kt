package kr.co.lokit.api.album.application

import kr.co.lokit.api.album.infrastructure.AlbumRepository
import org.springframework.stereotype.Service

@Service
class AlbumService(
    private val albumRepository: AlbumRepository
) {
}
