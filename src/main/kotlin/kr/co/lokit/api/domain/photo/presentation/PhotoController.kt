package kr.co.lokit.api.domain.photo.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.photo.application.port.`in`.CreatePhotoUseCase
import kr.co.lokit.api.domain.photo.application.port.`in`.GetPhotoDetailUseCase
import kr.co.lokit.api.domain.photo.application.port.`in`.UpdatePhotoUseCase
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.dto.PresignedUrlRequest
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.photo.mapping.toPhotoListResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("photos")
class PhotoController(
    private val createPhotoUseCase: CreatePhotoUseCase,
    private val getPhotoDetailUseCase: GetPhotoDetailUseCase,
    private val updatePhotoUseCase: UpdatePhotoUseCase,
) : PhotoApi {
    @GetMapping("album/{albumId}")
    @PreAuthorize("@permissionService.canAccessAlbum(#userId, #albumId)")
    override fun getPhotos(
        @CurrentUserId userId: Long,
        @PathVariable albumId: Long,
    ): PhotoListResponse =
        getPhotoDetailUseCase.getPhotosByAlbum(albumId, userId).toPhotoListResponse()

    @PostMapping("presigned-url")
    @ResponseStatus(HttpStatus.OK)
    override fun getPresignedUrl(
        @RequestBody @Valid request: PresignedUrlRequest,
    ): PresignedUrl = createPhotoUseCase.generatePresignedUrl(request.fileName, request.contentType)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody @Valid request: CreatePhotoRequest,
        @CurrentUserId userId: Long,
    ): IdResponse = createPhotoUseCase.create(request.toDomain(userId)).toIdResponse(Photo::id)

    @GetMapping("{id}")
    @PreAuthorize("@permissionService.canReadPhoto(#userId, #id)")
    override fun getPhotoDetail(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ): PhotoDetailResponse = getPhotoDetailUseCase.getPhotoDetail(id)

    @PutMapping("{id}")
    @PreAuthorize("@permissionService.canModifyPhoto(#userId, #id)")
    override fun update(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdatePhotoRequest,
    ): IdResponse =
        updatePhotoUseCase.update(id, request.albumId, request.description, request.longitude, request.latitude)
            .toIdResponse(Photo::id)

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionService.canDeletePhoto(#userId, #id)")
    override fun delete(
        @CurrentUserId userId: Long,
        @PathVariable id: Long,
    ) = updatePhotoUseCase.delete(id)
}
