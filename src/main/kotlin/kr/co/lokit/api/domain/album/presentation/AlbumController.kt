package kr.co.lokit.api.domain.album.presentation

import kr.co.lokit.api.domain.album.application.AlbumService
import org.springframework.web.bind.annotation.RestController

@RestController
class AlbumController(
    private val albumService: AlbumService
) {
}
