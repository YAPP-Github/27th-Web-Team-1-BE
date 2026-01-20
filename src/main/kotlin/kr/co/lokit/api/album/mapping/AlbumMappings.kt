package kr.co.lokit.api.album.mapping

import kr.co.lokit.api.album.domain.Album
import kr.co.lokit.api.album.infrastructure.AlbumEntity

fun AlbumEntity.toDomain(): Album = Album(
    id = this.id,
    title = this.title,
    imageCount = this.imageCount,
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
    title = this.title,
)
