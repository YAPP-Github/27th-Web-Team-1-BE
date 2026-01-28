package kr.co.lokit.api.domain.map.domain

data class AlbumBounds(
    val id: Long = 0,
    val albumId: Long,
    val minLongitude: Double,
    val maxLongitude: Double,
    val minLatitude: Double,
    val maxLatitude: Double,
) {
    val centerLongitude: Double
        get() = (minLongitude + maxLongitude) / 2

    val centerLatitude: Double
        get() = (minLatitude + maxLatitude) / 2

    fun expandedWith(longitude: Double, latitude: Double): AlbumBounds =
        copy(
            minLongitude = minOf(minLongitude, longitude),
            maxLongitude = maxOf(maxLongitude, longitude),
            minLatitude = minOf(minLatitude, latitude),
            maxLatitude = maxOf(maxLatitude, latitude),
        )

    companion object {
        fun createInitial(albumId: Long, longitude: Double, latitude: Double): AlbumBounds =
            AlbumBounds(
                albumId = albumId,
                minLongitude = longitude,
                maxLongitude = longitude,
                minLatitude = latitude,
                maxLatitude = latitude,
            )
    }
}
