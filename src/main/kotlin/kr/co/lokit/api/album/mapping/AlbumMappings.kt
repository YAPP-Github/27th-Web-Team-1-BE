package kr.co.lokit.api.album.mapping

import kr.co.lokit.api.album.domain.Album
import kr.co.lokit.api.album.infrastructure.AlbumEntity
import kr.co.lokit.api.photo.mapping.toDomain

fun AlbumEntity.toDomain(): Album = Album(
    id = this.id,
    title = this.title,
    photos = this.photos.map { it.toDomain() },
    photoCount = this.photoCount,
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    title = this.title,
)
