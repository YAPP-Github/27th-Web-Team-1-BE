package kr.co.lokit.api.domain.map.presentation

import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.domain.map.application.MapService
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("map")
class MapController(
    private val mapService: MapService,
) : MapApi {
    @GetMapping("home")
    override fun home(@CurrentUserId userId: Long, longitude: Double, latitude: Double): HomeResponse =
        mapService.home(userId, longitude, latitude)

    @GetMapping("/photos")
    override fun getPhotos(
        @RequestParam zoom: Int,
        @RequestParam bbox: String,
        @RequestParam(required = false) albumId: Long?,
    ): MapPhotosResponse {
        val bboxParsed = BBox.fromString(bbox)
        return mapService.getPhotos(zoom, bboxParsed, albumId)
    }

    @GetMapping("/clusters/{clusterId}/photos")
    override fun getClusterPhotos(
        @PathVariable clusterId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ClusterPhotosPageResponse = mapService.getClusterPhotos(clusterId, page, size)

    @GetMapping("/albums/{albumId}")
    override fun getAlbumMapInfo(
        @PathVariable albumId: Long,
    ): AlbumMapInfoResponse = mapService.getAlbumMapInfo(albumId)

    @GetMapping("/location")
    override fun getLocationInfo(
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
    ): LocationInfoResponse = mapService.getLocationInfo(longitude, latitude)

    @GetMapping("/places/search")
    override fun searchPlaces(
        @RequestParam query: String,
    ): PlaceSearchResponse = mapService.searchPlaces(query)
}
