package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.domain.photo.domain.PhotoDetailReadModel
import kr.co.lokit.api.domain.photo.domain.PresignedUpload
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl

fun PresignedUpload.toResponse(): PresignedUrl = PresignedUrl(presignedUrl = presignedUrl, objectUrl = objectUrl)

fun PhotoDetailReadModel.toResponse(): PhotoDetailResponse =
    PhotoDetailResponse(
        id = id,
        url = url,
        takenAt = takenAt,
        albumName = albumName,
        uploaderName = uploaderName,
        uploaderProfileImageUrl = uploaderProfileImageUrl,
        address = address,
        description = description,
        longitude = longitude,
        latitude = latitude,
    )
