package kr.co.lokit.api.domain.map.mapping

import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.BoundingBoxResponse
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

fun AlbumBounds.toBoundingBoxResponse(): BoundingBoxResponse =
    BoundingBoxResponse(
        west = minLongitude,
        south = minLatitude,
        east = maxLongitude,
        north = maxLatitude,
    )

fun AlbumBounds?.toAlbumMapInfoResponse(albumId: Long): AlbumMapInfoResponse =
    AlbumMapInfoResponse(
        albumId = albumId,
        centerLongitude = this?.centerLongitude,
        centerLatitude = this?.centerLatitude,
        boundingBox = this?.toBoundingBoxResponse(),
    )
