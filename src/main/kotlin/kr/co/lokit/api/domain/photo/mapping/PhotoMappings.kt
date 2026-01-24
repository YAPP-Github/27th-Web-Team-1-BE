package kr.co.lokit.api.domain.photo.mapping

import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity

fun CreatePhotoRequest.toDomain(): Photo = Photo(
    url = url,
    albumId = albumId,
    location = Location(longitude, latitude),
    description = description,
)

fun Photo.toEntity(album: AlbumEntity): PhotoEntity =
    PhotoEntity(
        url = this.url,
        album = album,
        longitude = this.location.longitude,
        latitude = this.location.latitude,
    ).apply {
        this.description = this@toEntity.description
    }

fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = this.id,
        url = this.url,
        albumId = this.album.id,
        location = Location(
            longitude = this.longitude,
            latitude = this.latitude,
        ),
        description = this.description,
    )
