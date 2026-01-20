package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain

fun AlbumEntity.toDomain(): Album = Album(
    id = this.id,
    title = this.title,
    photos = this.photos.map { it.toDomain() },
    photoCount = this.photoCount,
).apply {
    this.thumbnail = this@toDomain.thumbnail?.toDomain()
}

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    title = this.title,
)

fun AlbumRequest.toEntity(): AlbumEntity = AlbumEntity(
    title = this.title,
)
