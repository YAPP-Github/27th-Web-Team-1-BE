package kr.co.lokit.api.domain.photo.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.photo.application.PhotoService
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
    private val photoService: PhotoService,
) : PhotoApi {
    @GetMapping("album/{albumId}")
    override fun getPhotos(@PathVariable albumId: Long): PhotoListResponse =
        photoService.getPhotosByAlbum(albumId).toPhotoListResponse()

    @PostMapping("presigned-url")
    @ResponseStatus(HttpStatus.OK)
    override fun getPresignedUrl(
        @RequestBody @Valid request: PresignedUrlRequest,
    ): PresignedUrl = photoService.generatePresignedUrl(request.fileName, request.contentType)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody @Valid request: CreatePhotoRequest,
        @CurrentUserId userId: Long,
    ): IdResponse = photoService.create(request.toDomain(userId)).toIdResponse(Photo::id)

    @GetMapping("{id}")
    override fun getPhotoDetail(
        @PathVariable id: Long,
    ): PhotoDetailResponse = photoService.getPhotoDetail(id)

    @PutMapping("{id}")
    override fun update(
        @PathVariable id: Long,
        @RequestBody @Valid request: UpdatePhotoRequest,
        @CurrentUserId userId: Long,
    ): IdResponse =
        photoService.update(id, userId, request.albumId, request.description, request.longitude, request.latitude)
            .toIdResponse(Photo::id)

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(
        @PathVariable id: Long,
    ) = photoService.delete(id)
}
