package kr.co.lokit.api.domain.photo.presentation.mapping

import kr.co.lokit.api.common.permission.EditabilityPolicy
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.AlbumWithPhotosResponse
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.LocationResponse
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PhotoResponse

fun CreatePhotoRequest.toDomain(userId: Long): Photo =
    Photo(
        albumId = albumId,
        location = Location(longitude, latitude),
        description = description,
        uploadedById = userId,
        url = url,
        takenAt = takenAt,
)

fun Photo.toResponse(viewerUserId: Long): PhotoResponse =
    PhotoResponse(
        id = id,
        url = url,
        location =
            LocationResponse(
                longitude = location.longitude,
                latitude = location.latitude,
            ),
        description = description,
        takenAt = takenAt,
        isEditable = EditabilityPolicy.canEditOwnedResource(viewerUserId = viewerUserId, createdByUserId = uploadedById),
    )

fun Album.toAlbumWithPhotosResponse(viewerUserId: Long): AlbumWithPhotosResponse {
    val actualPhotoCount =
        if (isDefault) {
            photos.size
        } else {
            photoCount
        }

    return AlbumWithPhotosResponse(
        id = id,
        title = title,
        photoCount = actualPhotoCount,
        thumbnailUrl = thumbnail?.url,
        isEditable =
            EditabilityPolicy.canEditAlbum(
                viewerUserId = viewerUserId,
                createdByUserId = createdById,
                isDefault = isDefault,
            ),
        photos = photos.sortedByDescending { it.takenAt }.map { it.toResponse(viewerUserId) },
    )
}

fun List<Album>.toPhotoListResponse(viewerUserId: Long): PhotoListResponse =
    PhotoListResponse(
        albums = map { it.toAlbumWithPhotosResponse(viewerUserId) },
    )
