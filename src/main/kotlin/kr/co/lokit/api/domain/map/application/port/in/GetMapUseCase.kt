package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse

interface GetMapUseCase {
    fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Double,
        albumId: Long? = null,
        lastDataVersion: Long? = null,
    ): MapMeResponse

    fun getMe(
        userId: Long,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        zoom: Double,
        albumId: Long? = null,
        lastDataVersion: Long? = null,
    ): MapMeResponse

    fun getClusterPhotos(
        clusterId: String,
        userId: Long?,
    ): List<ClusterPhotoResponse>

    fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse
}
