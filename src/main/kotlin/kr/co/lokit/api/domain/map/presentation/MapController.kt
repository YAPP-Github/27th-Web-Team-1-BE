package kr.co.lokit.api.domain.map.presentation

import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("map")
class MapController(
    private val getMapUseCase: GetMapUseCase,
    private val searchLocationUseCase: SearchLocationUseCase,
) : MapApi {
    @GetMapping("home")
    override fun home(@CurrentUserId userId: Long, longitude: Double, latitude: Double): HomeResponse =
        getMapUseCase.home(userId, longitude, latitude)

    @GetMapping("me")
    override fun getMe(
        @CurrentUserId userId: Long,
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
        @RequestParam zoom: Int,
        @RequestParam bbox: String,
        @RequestParam(required = false) albumId: Long?,
    ): MapMeResponse {
        return getMapUseCase.getMe(userId, longitude, latitude, zoom, BBox.fromString(bbox), albumId)
    }

    @GetMapping("photos")
    override fun getPhotos(
        @CurrentUserId userId: Long,
        @RequestParam zoom: Int,
        @RequestParam bbox: String,
        @RequestParam(required = false) albumId: Long?,
    ): MapPhotosResponse {
        return getMapUseCase.getPhotos(zoom, BBox.fromString(bbox), userId, albumId)
    }

    @GetMapping("clusters/{clusterId}/photos")
    override fun getClusterPhotos(
        @CurrentUserId userId: Long,
        @PathVariable clusterId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ClusterPhotosPageResponse = getMapUseCase.getClusterPhotos(clusterId, userId, page, size)

    @GetMapping("albums/{albumId}")
    @PreAuthorize("@permissionService.canAccessAlbum(#userId, #albumId)")
    override fun getAlbumMapInfo(
        @CurrentUserId userId: Long,
        @PathVariable albumId: Long,
    ): AlbumMapInfoResponse = getMapUseCase.getAlbumMapInfo(albumId)

    @GetMapping("location")
    override fun getLocationInfo(
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
    ): LocationInfoResponse = searchLocationUseCase.getLocationInfo(longitude, latitude)

    @GetMapping("places/search")
    override fun searchPlaces(
        @RequestParam query: String,
    ): PlaceSearchResponse = searchLocationUseCase.searchPlaces(query)
}
