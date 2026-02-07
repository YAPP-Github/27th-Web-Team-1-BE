package kr.co.lokit.api.domain.map.application.port

interface MapQueryPort {
    fun findClustersWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        gridSize: Double,
        coupleId: Long? = null,
        albumId: Long? = null,
    ): List<ClusterProjection>

    fun findPhotosWithinBBox(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        coupleId: Long? = null,
        albumId: Long? = null,
    ): List<PhotoProjection>

    fun findPhotosInGridCell(
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        coupleId: Long? = null,
    ): List<ClusterPhotoProjection>
}
