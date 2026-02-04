package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
) {
    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key = "#cacheKey",
        unless = "#result.clusters?.isEmpty() == true && #result.photos?.isEmpty() == true"
    )
    fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        userId: Long?,
        albumId: Long?,
        cacheKey: String,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom)

        val clusters =
            mapQueryPort.findClustersWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                gridSize = gridSize,
                userId = userId,
                albumId = albumId,
            )

        return MapPhotosResponse(
            clusters = clusters.map { it.toResponse(zoom) },
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key = "#cacheKey",
        unless = "#result.clusters?.isEmpty() == true && #result.photos?.isEmpty() == true"
    )
    fun getIndividualPhotos(
        bbox: BBox,
        userId: Long?,
        albumId: Long?,
        cacheKey: String,
    ): MapPhotosResponse {
        val photos = mapQueryPort.findPhotosWithinBBox(
            west = bbox.west,
            south = bbox.south,
            east = bbox.east,
            north = bbox.north,
            userId = userId,
            albumId = albumId,
        )

        return MapPhotosResponse(
            photos = photos.map { it.toMapPhotoResponse() },
        )
    }

    fun buildCacheKey(zoom: Int, bbox: BBox, userId: Long?, albumId: Long?): String {
        val gridSize = 0.001
        val centerLon = (bbox.west + bbox.east) / 2.0
        val centerLat = (bbox.south + bbox.north) / 2.0
        val gridX = (centerLon / gridSize).toLong()
        val gridY = (centerLat / gridSize).toLong()

        return buildString {
            append(zoom)
            append(":")
            append(gridX)
            append(":")
            append(gridY)
            if (userId != null) {
                append(":u")
                append(userId)
            }
            if (albumId != null) {
                append(":a")
                append(albumId)
            }
        }
    }
}
