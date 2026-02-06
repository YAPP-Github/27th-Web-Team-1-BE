package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotoResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse

interface GetMapUseCase {
    fun home(userId: Long, longitude: Double, latitude: Double): HomeResponse
    fun getPhotos(zoom: Int, bbox: BBox, userId: Long? = null, albumId: Long? = null): MapPhotosResponse
    fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Int,
        bbox: BBox,
        albumId: Long? = null,
        lastDataVersion: Long? = null,
    ): MapMeResponse

    fun getClusterPhotos(clusterId: String, userId: Long?): List<ClusterPhotoResponse>
    fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse
}
