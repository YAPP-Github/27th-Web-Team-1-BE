package kr.co.lokit.api.domain.photo.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity

fun Photo.toEntity(album: AlbumEntity): PhotoEntity =
    PhotoEntity(
        url = this.url,
        album = album,
        longitude = this.location.longitude,
        latitude = this.location.latitude,
    ).apply {
        this.description = this@toEntity.description
    }

fun PhotoEntity.toDomain(album: Album): Photo =
    Photo(
        id = this.id,
        url = this.url,
        album = album,
        location = Location(
            longitude = this.longitude,
            latitude = this.latitude,
        ),
        description = this.description,
    )
