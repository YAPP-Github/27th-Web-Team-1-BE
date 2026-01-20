package kr.co.lokit.api.photo.mapping

import kr.co.lokit.api.album.infrastructure.AlbumEntity
import kr.co.lokit.api.album.mapping.toDomain
import kr.co.lokit.api.photo.domain.Photo
import kr.co.lokit.api.photo.infrastructure.PhotoEntity

fun Photo.toEntity(album: AlbumEntity): PhotoEntity =
    PhotoEntity(
        url = this.url,
        album = album,
        longitude = this.longitude,
        latitude = this.latitude,
    )

fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = this.id,
        url = this.url,
        album = this.album.toDomain(),
        longitude = this.longitude,
        latitude = this.latitude,
    ).apply {
        description = this@toDomain.description
    }
