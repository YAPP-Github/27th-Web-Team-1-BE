package kr.co.lokit.api.domain.map.application.mapping

import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.AlbumMapInfoReadModel
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.BoundingBoxReadModel
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.ClusterPhotoReadModel
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import kr.co.lokit.api.domain.map.domain.MapPhotoReadModel

fun ClusterProjection.toClusterReadModel(zoom: Int): ClusterReadModel =
    ClusterReadModel(
        clusterId = ClusterId.format(zoom, cellX, cellY),
        count = count,
        thumbnailUrl = thumbnailUrl,
        longitude = centerLongitude,
        latitude = centerLatitude,
        takenAt = takenAt,
    )

fun PhotoProjection.toMapPhotoReadModel(): MapPhotoReadModel =
    MapPhotoReadModel(
        id = id,
        thumbnailUrl = url,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
    )

fun ClusterPhotoProjection.toClusterPhotoReadModel(): ClusterPhotoReadModel =
    ClusterPhotoReadModel(
        id = id,
        url = url,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
        address = address,
    )

fun List<ClusterPhotoProjection>.toClusterPhotoReadModels(): List<ClusterPhotoReadModel> =
    map { it.toClusterPhotoReadModel() }

fun BBox.toBoundingBoxReadModel(): BoundingBoxReadModel =
    BoundingBoxReadModel(
        west = west,
        south = south,
        east = east,
        north = north,
    )

fun AlbumBounds.toBoundingBoxReadModel(): BoundingBoxReadModel =
    BoundingBoxReadModel(
        west = minLongitude,
        south = minLatitude,
        east = maxLongitude,
        north = maxLatitude,
    )

fun AlbumBounds?.toAlbumMapReadModel(albumId: Long): AlbumMapInfoReadModel =
    AlbumMapInfoReadModel(
        albumId = albumId,
        centerLongitude = this?.centerLongitude,
        centerLatitude = this?.centerLatitude,
        boundingBox = this?.toBoundingBoxReadModel(),
    )
