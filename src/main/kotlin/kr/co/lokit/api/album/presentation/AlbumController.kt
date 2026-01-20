package kr.co.lokit.api.album.presentation

import kr.co.lokit.api.album.application.AlbumService
import org.springframework.web.bind.annotation.RestController

@RestController
class AlbumController(
    private val albumService: AlbumService
) {
}
