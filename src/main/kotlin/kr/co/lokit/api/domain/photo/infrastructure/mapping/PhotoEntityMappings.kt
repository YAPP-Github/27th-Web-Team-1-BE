package kr.co.lokit.api.domain.photo.infrastructure.mapping

import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun Photo.toEntity(
    album: AlbumEntity,
    uploadedBy: UserEntity,
): PhotoEntity =
    PhotoEntity(
        url = url,
        album = album,
        location = PhotoEntity.createPoint(location.longitude, location.latitude),
        uploadedBy = uploadedBy,
        address = requireNotNull(address) { "Photo.address must be initialized before persistence" },
    ).apply {
        this.description = this@toEntity.description
        this.takenAt = this@toEntity.takenAt
        this.coupleId = album.couple.nonNullId()
    }

fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = nonNullId(),
        albumId = album.nonNullId(),
        coupleId = coupleId,
        location = Location(longitude = longitude, latitude = latitude),
        description = description,
        url = url,
        uploadedById = uploadedBy.nonNullId(),
        takenAt = takenAt,
        address = address,
    )

fun PhotoEntity.toPhotoDetail(): PhotoDetail =
    PhotoDetail(
        id = nonNullId(),
        url = url,
        takenAt = takenAt,
        albumName = album.title,
        uploadedById = uploadedBy.nonNullId(),
        uploaderName = uploadedBy.name,
        uploaderProfileImageUrl = uploadedBy.profileImageUrl,
        location =
            Location(
                longitude = longitude,
                latitude = latitude,
            ),
        description = description,
    )
