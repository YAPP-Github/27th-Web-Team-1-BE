package kr.co.lokit.api.domain.photo.mapping

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
    ).apply {
        uploadedById = userId
        takenAt = this@toDomain.takenAt
    }

fun Photo.toEntity(album: AlbumEntity, uploadedBy: UserEntity): PhotoEntity =
    PhotoEntity(
        url = this.url!!,
        album = album,
        location = PhotoEntity.createPoint(this.location.longitude, this.location.latitude),
        uploadedBy = uploadedBy,
    ).apply {
        this.description = this@toEntity.description
        this.takenAt = this@toEntity.takenAt
    }

fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = this.id,
        albumId = this.album.id,
        location = Location(longitude = this.longitude, latitude = this.latitude),
        description = this.description,
    ).apply {
        uploadedById = this@toDomain.uploadedBy.id
        takenAt = this@toDomain.takenAt
    }

fun PhotoEntity.toResponse(): PhotoResponse =
    PhotoResponse(
        id = this.id,
        url = this.url,
        location =
            LocationResponse(
                longitude = this.longitude,
                latitude = this.latitude,
            ),
        description = this.description,
    )

fun AlbumEntity.toAlbumWithPhotosResponse(): AlbumWithPhotosResponse =
    AlbumWithPhotosResponse(
        id = this.id,
        title = this.title,
        photoCount = this.photoCount,
        thumbnailUrl = this.thumbnail?.url,
        photos = this.photos.map { it.toResponse() },
    )

fun List<AlbumEntity>.toPhotoListResponse(): PhotoListResponse =
    PhotoListResponse(
        albums = this.map { it.toAlbumWithPhotosResponse() },
    )

fun PhotoEntity.toPhotoDetail(): PhotoDetail =
    PhotoDetail(
        id = this.id,
        url = this.url,
        takenAt = this.takenAt,
        albumName = this.album.title,
        uploaderName = this.uploadedBy.name,
        location = Location(
            longitude = this.longitude,
            latitude = this.latitude,
        ),
        description = this.description,
    )
