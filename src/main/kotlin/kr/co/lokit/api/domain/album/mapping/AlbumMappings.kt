package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.domain.AlbumUser
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain

fun AlbumEntity.toDomain(): Album =
    Album(
        id = this.id,
        title = this.title,
        photoCount = this.photoCount,
    ).apply {
        this.inviteCode = this@toDomain.inviteCode
        this.photos = this@toDomain.photos.map { it.toDomain(this) }
        this.thumbnail = this@toDomain.thumbnail?.toDomain(this)
        this.albumUsers = this@toDomain.albumUsers.map {
            AlbumUser(
                id = it.id,
                userId = it.user.id
            )
        }
    }

fun Album.toEntity(): AlbumEntity =
    AlbumEntity(
        title = this.title,
    )

fun AlbumRequest.toDomain(userId: Long): Album =
    Album(
        title = this.title,
    ).apply {
        this.albumUsers = listOf(
            AlbumUser(
                userId = userId,
            )
        )
    }

fun List<Album>.toSelectableResponse(): SelectableAlbumResponse =
    SelectableAlbumResponse(map {
        SelectableAlbumResponse.SelectableAlbum(
            id = it.id,
            title = it.title,
            photoCount = it.photoCount,
            thumbnailUrl = it.thumbnail?.url,
        )
    })
