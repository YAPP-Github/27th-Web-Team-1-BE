package kr.co.lokit.api.domain.photo.mapping

import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity

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
