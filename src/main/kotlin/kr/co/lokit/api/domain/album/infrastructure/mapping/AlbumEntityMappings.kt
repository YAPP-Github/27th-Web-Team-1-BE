package kr.co.lokit.api.domain.album.infrastructure.mapping

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.photo.infrastructure.mapping.toDomain
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

fun AlbumEntity.toDomain(): Album =
    Album(
        id = nonNullId(),
        title = title,
        coupleId = couple.nonNullId(),
        createdById = createdBy.nonNullId(),
        photoCount = photoCount,
        isDefault = isDefault,
    ).apply {
        this.photos = this@toDomain.photos.map { it.toDomain() }
    }

fun Album.toEntity(
    couple: CoupleEntity,
    createdBy: UserEntity,
): AlbumEntity =
    AlbumEntity(
        title = title,
        couple = couple,
        createdBy = createdBy,
        isDefault = isDefault,
    )
