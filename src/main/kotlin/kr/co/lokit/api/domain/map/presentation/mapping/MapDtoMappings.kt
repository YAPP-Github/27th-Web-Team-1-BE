package kr.co.lokit.api.domain.map.presentation.mapping

import kr.co.lokit.api.domain.map.domain.AlbumMapInfoReadModel
import kr.co.lokit.api.domain.map.domain.AlbumThumbnailsReadModel
import kr.co.lokit.api.domain.map.domain.ClusterPhotos
import kr.co.lokit.api.domain.map.domain.BoundingBoxReadModel
import kr.co.lokit.api.domain.map.domain.ClusterPhotoReadModel
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.MapMeReadModel
import kr.co.lokit.api.domain.map.domain.MapPhotoReadModel
import kr.co.lokit.api.domain.map.domain.PlaceSearchReadModel
import kr.co.lokit.api.domain.map.domain.PlaceReadModel
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.BoundingBoxResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse

fun MapMeReadModel.toResponse(): MapMeResponse =
    MapMeResponse(
        location = location.toResponse(),
        boundingBox = boundingBox.toResponse(),
        totalHistoryCount = totalHistoryCount,
        albums = albums.asList().map { it.toResponse() },
        dataVersion = dataVersion,
        clusters = clusters?.asList()?.map { it.toResponse() },
        photos = photos?.asList()?.map { it.toResponse() },
    )

fun ClusterPhotos.toResponse(): List<ClusterPhotoResponse> = asList().map { it.toResponse() }

fun AlbumMapInfoReadModel.toResponse(): AlbumMapInfoResponse =
    AlbumMapInfoResponse(
        albumId = albumId,
        centerLongitude = centerLongitude,
        centerLatitude = centerLatitude,
        boundingBox = boundingBox?.toResponse(),
    )

fun LocationInfoReadModel.toResponse(): LocationInfoResponse =
    LocationInfoResponse(
        address = address,
        roadName = roadName,
        placeName = placeName,
        regionName = regionName,
    )

fun PlaceSearchReadModel.toResponse(): PlaceSearchResponse =
    PlaceSearchResponse(places = places.asList().map { it.toResponse() })

private fun ClusterReadModel.toResponse(): ClusterResponse =
    ClusterResponse(
        clusterId = clusterId,
        count = count,
        thumbnailUrl = thumbnailUrl,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
    )

private fun MapPhotoReadModel.toResponse(): MapPhotoResponse =
    MapPhotoResponse(
        id = id,
        thumbnailUrl = thumbnailUrl,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
    )

private fun ClusterPhotoReadModel.toResponse(): ClusterPhotoResponse =
    ClusterPhotoResponse(
        id = id,
        url = url,
        longitude = longitude,
        latitude = latitude,
        takenAt = takenAt,
        address = address,
    )

private fun BoundingBoxReadModel.toResponse(): BoundingBoxResponse =
    BoundingBoxResponse(
        west = west,
        south = south,
        east = east,
        north = north,
    )

private fun PlaceReadModel.toResponse(): PlaceResponse =
    PlaceResponse(
        placeName = placeName,
        address = address,
        roadAddress = roadAddress,
        longitude = longitude,
        latitude = latitude,
        category = category,
    )

private fun AlbumThumbnailsReadModel.toResponse(): HomeResponse.Companion.AlbumThumbnails =
    HomeResponse.Companion.AlbumThumbnails(
        id = id,
        title = title,
        photoCount = photoCount,
        thumbnailUrls = thumbnailUrls.asList(),
    )
