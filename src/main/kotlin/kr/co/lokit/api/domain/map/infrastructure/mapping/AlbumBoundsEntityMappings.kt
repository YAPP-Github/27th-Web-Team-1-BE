package kr.co.lokit.api.domain.map.infrastructure.mapping

import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsEntity

fun AlbumBounds.toEntity(): AlbumBoundsEntity =
    AlbumBoundsEntity(
        standardId = standardId,
        idType = idType,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
    )

fun AlbumBoundsEntity.toDomain(): AlbumBounds =
    AlbumBounds(
        id = nonNullId(),
        standardId = standardId,
        idType = idType,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
    )
