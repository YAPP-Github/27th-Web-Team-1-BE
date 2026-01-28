package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridConfig
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsRepository
import kr.co.lokit.api.domain.map.infrastructure.MapRepository
import kr.co.lokit.api.domain.map.infrastructure.geocoding.MapClient
import kr.co.lokit.api.domain.map.mapping.toAlbumMapInfoResponse
import kr.co.lokit.api.domain.map.mapping.toClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MapService(
    private val mapRepository: MapRepository,
    private val albumBoundsRepository: AlbumBoundsRepository,
    private val mapClient: MapClient,
) {
    companion object {
        private const val CLUSTER_ZOOM_THRESHOLD = 15
    }

    @Transactional(readOnly = true)
    fun getPhotos(
        zoom: Int,
        bbox: BBox,
        albumId: Long? = null,
    ): MapPhotosResponse =
        if (zoom < CLUSTER_ZOOM_THRESHOLD) {
            getClusteredPhotos(zoom, bbox, albumId)
        } else {
            getIndividualPhotos(bbox, albumId)
        }

    private fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        albumId: Long? = null,
    ): MapPhotosResponse {
        val gridSize = GridConfig.getGridSize(zoom)

        val clusters =
            mapRepository.findClustersWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                gridSize = gridSize,
                albumId = albumId,
            )

        return MapPhotosResponse(
            clusters = clusters.map { it.toResponse(zoom) },
        )
    }

    private fun getIndividualPhotos(bbox: BBox, albumId: Long? = null): MapPhotosResponse {
        val photos =
            mapRepository.findPhotosWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                albumId = albumId,
            )

        return MapPhotosResponse(
            photos = photos.map { it.toMapPhotoResponse() },
        )
    }

    @Transactional(readOnly = true)
    fun getClusterPhotos(
        clusterId: String,
        page: Int,
        size: Int,
    ): ClusterPhotosPageResponse {
        val gridCell = ClusterId.parse(clusterId)
        val bbox = gridCell.toBBox()

        return mapRepository
            .findPhotosInGridCell(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                page = page,
                size = size,
            ).toClusterPhotosPageResponse()
    }

    @Transactional(readOnly = true)
    fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse {
        val bounds = albumBoundsRepository.findByAlbumId(albumId)
        return bounds.toAlbumMapInfoResponse(albumId)
    }

    fun getLocationInfo(longitude: Double, latitude: Double): LocationInfoResponse =
        mapClient.reverseGeocode(longitude, latitude)

    fun searchPlaces(query: String): PlaceSearchResponse =
        PlaceSearchResponse(places = mapClient.searchPlaces(query))
}
