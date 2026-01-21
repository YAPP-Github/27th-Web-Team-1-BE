package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain

fun AlbumEntity.toDomain(): Album = Album(
    title = this.title,
).apply {
    this.id = this@toDomain.id
    this.photoCount = this@toDomain.photoCount
    this.photos = this@toDomain.photos.map { it.toDomain(this) }
    this.thumbnail = this@toDomain.thumbnail?.toDomain(this)
}

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    title = this.title,
)

fun AlbumRequest.toDomain(): Album = Album(
    title = this.title,
)
