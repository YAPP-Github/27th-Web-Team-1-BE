package kr.co.lokit.api.domain.photo.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.dto.AlbumWithPhotosResponse
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.LocationResponse
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PhotoResponse
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun CreatePhotoRequest.toDomain(userId: Long): Photo =
    Photo(
        albumId = albumId,
        location = Location(longitude, latitude),
        description = description,
        uploadedById = userId,
        url = url,
        takenAt = this@toDomain.takenAt,
    )

fun Photo.toEntity(
    album: AlbumEntity,
    uploadedBy: UserEntity,
): PhotoEntity =
    PhotoEntity(
        url = this.url,
        album = album,
        location = PhotoEntity.createPoint(this.location.longitude, this.location.latitude),
        uploadedBy = uploadedBy,
        address = requireNotNull(this.address) { "Photo.address must be initialized before persistence" },
    ).apply {
        this.description = this@toEntity.description
        this.takenAt = this@toEntity.takenAt
        this.coupleId = album.couple.nonNullId()
    }

fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = this.nonNullId(),
        albumId = this.album.nonNullId(),
        coupleId = this.coupleId,
        location = Location(longitude = this.longitude, latitude = this.latitude),
        description = this.description,
        url = this.url,
        uploadedById = this.uploadedBy.nonNullId(),
        takenAt = this.takenAt,
        address = this.address,
    )

fun Photo.toResponse(): PhotoResponse =
    PhotoResponse(
        id = this.id,
        url = this.url,
        location =
            LocationResponse(
                longitude = this.location.longitude,
                latitude = this.location.latitude,
            ),
        description = this.description,
        takenAt = this.takenAt,
    )

fun Album.toAlbumWithPhotosResponse(): AlbumWithPhotosResponse {
    val actualPhotoCount =
        if (this.isDefault) {
            this.photos.size
        } else {
            this.photoCount
        }

    return AlbumWithPhotosResponse(
        id = this.id,
        title = this.title,
        photoCount = actualPhotoCount,
        thumbnailUrl = this.thumbnail?.url,
        photos = this.photos.sortedByDescending { it.takenAt }.map { it.toResponse() },
    )
}

fun List<Album>.toPhotoListResponse(): PhotoListResponse =
    PhotoListResponse(
        albums = this.map { it.toAlbumWithPhotosResponse() },
    )

fun PhotoEntity.toPhotoDetail(): PhotoDetail =
    PhotoDetail(
        id = this.nonNullId(),
        url = this.url,
        takenAt = this.takenAt,
        albumName = this.album.title,
        uploadedById = this.uploadedBy.nonNullId(),
        uploaderName = this.uploadedBy.name,
        uploaderProfileImageUrl = this.uploadedBy.profileImageUrl,
        location =
            Location(
                longitude = this.longitude,
                latitude = this.latitude,
            ),
        description = this.description,
    )
