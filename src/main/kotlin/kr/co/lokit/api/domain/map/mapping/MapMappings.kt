package kr.co.lokit.api.domain.map.mapping

import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.dto.BoundingBoxResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.MapPhotoResponse

fun ClusterProjection.toResponse(zoom: Int): ClusterResponse =
    ClusterResponse(
        clusterId = ClusterId.format(zoom, cellX, cellY),
        count = count,
        thumbnailUrl = thumbnailUrl,
        longitude = centerLongitude,
        latitude = centerLatitude,
    )

fun PhotoProjection.toMapPhotoResponse(): MapPhotoResponse =
    MapPhotoResponse(
        id = id,
        thumbnailUrl = url,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
    )

fun ClusterPhotoProjection.toClusterPhotoResponse(): ClusterPhotoResponse =
    ClusterPhotoResponse(
        id = id,
        url = url,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
        address = address,
    )

fun List<ClusterPhotoProjection>.toClusterPhotosPageResponse(): List<ClusterPhotoResponse> =
    map { it.toClusterPhotoResponse() }

fun BBox.toResponse(): BoundingBoxResponse =
    BoundingBoxResponse(
        west = west,
        south = south,
        east = east,
        north = north,
    )
