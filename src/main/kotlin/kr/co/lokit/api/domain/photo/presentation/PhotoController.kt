package kr.co.lokit.api.domain.photo.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.config.security.CurrentUserId
import kr.co.lokit.api.domain.photo.application.PhotoService
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.dto.PresignedUrlRequest
import kr.co.lokit.api.domain.photo.mapping.toDomain
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("photos")
class PhotoController(
    private val photoService: PhotoService,
) : PhotoApi {
    @GetMapping
    override fun getPhotos(albumId: Long): PhotoListResponse = photoService.getPhotosByAlbum()

    @PostMapping("presigned-url")
    @ResponseStatus(HttpStatus.OK)
    override fun getPresignedUrl(
        @RequestBody @Valid request: PresignedUrlRequest,
        @CurrentUserId userId: Long,
    ): PresignedUrl = photoService.generatePresignedUrl(request.fileName, request.contentType, userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody @Valid request: CreatePhotoRequest,
        @CurrentUserId userId: Long,
    ): IdResponse = photoService.create(request.toDomain()).toIdResponse(Photo::id)
}
