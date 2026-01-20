package kr.co.lokit.api.domain.album.presentation

import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class AlbumController(
    private val albumService: AlbumService
) : AlbumApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@RequestBody albumRequest: AlbumRequest): IdResponse = albumService.create(albumRequest)
}
