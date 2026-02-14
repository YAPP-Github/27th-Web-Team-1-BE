package kr.co.lokit.api.domain.album.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.photo.mapping.toDomain
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun AlbumEntity.toDomain(): Album =
    Album(
        id = this.nonNullId(),
        title = this.title,
        coupleId = this.couple.nonNullId(),
        createdById = this.createdBy.nonNullId(),
        photoCount = this.photoCount,
        isDefault = this.isDefault,
    ).apply {
        this.photos = this@toDomain.photos.map { it.toDomain() }
    }

fun Album.toEntity(
    couple: CoupleEntity,
    createdBy: UserEntity,
): AlbumEntity =
    AlbumEntity(
        title = this.title,
        couple = couple,
        createdBy = createdBy,
        isDefault = this.isDefault,
    )

fun AlbumRequest.toDomain(): Album =
    Album(
        title = this.title,
    )

fun List<Album>.toSelectableResponse(): SelectableAlbumResponse =
    SelectableAlbumResponse(
        map {
            val actualPhotoCount =
                if (it.isDefault) {
                    it.photos.size
                } else {
                    it.photoCount
                }

            SelectableAlbumResponse.SelectableAlbum(
                id = it.id,
                title = it.title,
                photoCount = actualPhotoCount,
                thumbnailUrl = it.thumbnail?.url,
            )
        },
    )
