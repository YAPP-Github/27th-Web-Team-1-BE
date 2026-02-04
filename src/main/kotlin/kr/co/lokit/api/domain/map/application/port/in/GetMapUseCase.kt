package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.ClusterPhotosPageResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse

interface GetMapUseCase {
    fun home(userId: Long, longitude: Double, latitude: Double): HomeResponse
    fun getPhotos(zoom: Int, bbox: BBox, albumId: Long? = null): MapPhotosResponse
    fun getClusterPhotos(clusterId: String, page: Int, size: Int): ClusterPhotosPageResponse
    fun getAlbumMapInfo(albumId: Long): AlbumMapInfoResponse
}
