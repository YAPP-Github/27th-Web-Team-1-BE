package kr.co.lokit.api.domain.album.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.mapping.toDomain
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("albums")
class AlbumController(
    private val albumService: AlbumService
) : AlbumApi {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@RequestBody @Valid albumRequest: AlbumRequest): IdResponse =
        albumService.create(albumRequest.toDomain())
}
