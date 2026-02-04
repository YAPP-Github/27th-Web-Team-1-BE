package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.common.concurrency.StructuredConcurrency
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.map.mapping.toAlbumMapInfoResponse
import kr.co.lokit.api.domain.map.mapping.toClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class MapQueryService(
    private val mapQueryPort: MapQueryPort,
    private val albumBoundsRepository: AlbumBoundsRepositoryPort,
    private val albumRepository: AlbumRepositoryPort,
    private val mapClientPort: MapClientPort,
    private val transactionTemplate: TransactionTemplate,
) : GetMapUseCase, SearchLocationUseCase {
    override fun home(userId: Long, longitude: Double, latitude: Double): HomeResponse {
        val bBox = BBox.fromCenter(GridValues.HOME_ZOOM_LEVEL, longitude, latitude)

        return StructuredConcurrency.run { scope ->
            val locationFuture = scope.fork { mapClientPort.reverseGeocode(longitude, latitude) }
            val albumsFuture = scope.fork {
                transactionTemplate.execute {
                    albumRepository.findAllByUserId(userId).sortedByDescending { it.isDefault }
                }!!
            }

            HomeResponse.of(locationFuture.get(), albumsFuture.get(), bBox)
        }
    }

    @Transactional(readOnly = true)
    override fun getPhotos(
        zoom: Int,
        bbox: BBox,
        albumId: Long?
    ): MapPhotosResponse {
        return if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) {
            getClusteredPhotos(zoom, bbox, albumId)
        } else {
            getIndividualPhotos(bbox, albumId)
        }
    }

    private fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        albumId: Long? = null,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom)

        val clusters =
            mapQueryPort.findClustersWithinBBox(
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
            mapQueryPort.findPhotosWithinBBox(
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
    override fun getClusterPhotos(
        clusterId: String,
        page: Int,
        size: Int,
    ): ClusterPhotosPageResponse {
        val gridCell = ClusterId.parse(clusterId)
        val bbox = gridCell.toBBox()

        return mapQueryPort
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
    override fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse {
        val bounds = albumBoundsRepository.findByAlbumIdOrNull(albumId)
        return bounds.toAlbumMapInfoResponse(albumId)
    }

    override fun getLocationInfo(longitude: Double, latitude: Double): LocationInfoResponse {
        val raw = mapClientPort.reverseGeocode(longitude, latitude)
        val header = AddressFormatter.toRoadHeader(raw.address, raw.roadName)
        return raw.copy(address = header)
    }

    override fun searchPlaces(query: String): PlaceSearchResponse =
        PlaceSearchResponse(places = mapClientPort.searchPlaces(query))
}
