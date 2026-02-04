package kr.co.lokit.api.domain.album.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.album.application.port.`in`.CreateAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.GetAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.UpdateAlbumUseCase
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.dto.UpdateAlbumTitleRequest
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toSelectableResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
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
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val getAlbumUseCase: GetAlbumUseCase,
    private val updateAlbumUseCase: UpdateAlbumUseCase,
) : AlbumApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@CurrentUserId userId: Long, @RequestBody @Valid albumRequest: AlbumRequest): IdResponse =
        createAlbumUseCase.create(albumRequest.toDomain(), userId).toIdResponse(Album::id)

    @GetMapping("selectable")
    @ResponseStatus(HttpStatus.OK)
    override fun getSelectableAlbums(@CurrentUserId userId: Long): SelectableAlbumResponse =
        getAlbumUseCase.getSelectableAlbums(userId).toSelectableResponse()

    @PatchMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@permissionService.canModifyAlbum(#userId, #id)")
    override fun updateTitle(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdateAlbumTitleRequest,
    ): IdResponse =
        updateAlbumUseCase.updateTitle(id, request.title).toIdResponse(Album::id)

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionService.canDeleteAlbum(#userId, #id)")
    override fun delete(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ) = updateAlbumUseCase.delete(id)
}
