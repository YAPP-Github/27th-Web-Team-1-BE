package kr.co.lokit.api.domain.map.presentation

import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.map.presentation.mapping.toResponse
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
    @GetMapping("me")
    override fun getMe(
        @CurrentUserId userId: Long,
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
        @RequestParam zoom: Double,
        @RequestParam(required = false) albumId: Long?,
        @RequestParam(required = false) lastDataVersion: Long?,
    ): MapMeResponse =
        getMapUseCase.getMe(
            userId,
            longitude,
            latitude,
            zoom,
            albumId,
            lastDataVersion,
        ).toResponse()

    @GetMapping("clusters/{clusterId}/photos")
    override fun getClusterPhotos(
        @CurrentUserId userId: Long,
        @PathVariable clusterId: String,
    ): List<ClusterPhotoResponse> = getMapUseCase.getClusterPhotos(clusterId, userId).toResponse()

    @GetMapping("albums/{albumId}")
    @PreAuthorize("@permissionService.canAccessAlbum(#userId, #albumId)")
    override fun getAlbumMapInfo(
        @CurrentUserId userId: Long,
        @PathVariable albumId: Long,
    ): AlbumMapInfoResponse = getMapUseCase.getAlbumMapInfo(albumId).toResponse()

    @GetMapping("location")
    override fun getLocationInfo(
        @RequestParam longitude: Double,
        @RequestParam latitude: Double,
    ): LocationInfoResponse = searchLocationUseCase.getLocationInfo(longitude, latitude).toResponse()

    @GetMapping("places/search")
    override fun searchPlaces(
        @RequestParam query: String,
    ): PlaceSearchResponse = searchLocationUseCase.searchPlaces(query).toResponse()
}
