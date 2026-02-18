package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.domain.AlbumMapInfoReadModel
import kr.co.lokit.api.domain.map.domain.ClusterPhotos
import kr.co.lokit.api.domain.map.domain.MapMeReadModel

interface GetMapUseCase {
    fun getMe(
        userId: Long,
        longitude: Double,
        latitude: Double,
        zoom: Double,
        albumId: Long? = null,
        lastDataVersion: Long? = null,
    ): MapMeReadModel

    fun getClusterPhotos(
        clusterId: String,
        userId: Long?,
    ): ClusterPhotos

    fun getAlbumMapInfo(albumId: Long): AlbumMapInfoReadModel
}
