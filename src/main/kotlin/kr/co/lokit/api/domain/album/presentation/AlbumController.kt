package kr.co.lokit.api.domain.album.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.config.security.CurrentUserId
import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.dto.UpdateAlbumTitleRequest
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toSelectableResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("albums")
class AlbumController(
    private val albumService: AlbumService,
) : AlbumApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@RequestBody @Valid albumRequest: AlbumRequest): IdResponse =
        albumService.create(albumRequest.toDomain(albumRequest.workspaceId)).toIdResponse(Album::id)

    @GetMapping("selectable")
    @ResponseStatus(HttpStatus.OK)
    override fun getSelectableAlbums(@CurrentUserId userId: Long): SelectableAlbumResponse =
        albumService.getSelectableAlbums(userId).toSelectableResponse()

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    override fun updateTitle(
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateAlbumTitleRequest,
    ): IdResponse =
        albumService.updateTitle(id, request.title).toIdResponse(Album::id)

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(@PathVariable id: Long) =
        albumService.delete(id)
}
