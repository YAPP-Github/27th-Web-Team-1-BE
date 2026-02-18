package kr.co.lokit.api.domain.map.domain

import java.time.LocalDateTime

data class ClusterReadModel(
    val clusterId: String,
    val count: Int,
    val thumbnailUrl: String,
    val longitude: Double,
    val latitude: Double,
    val takenAt: LocalDateTime? = null,
)

@JvmInline
value class Clusters private constructor(
    private val items: List<ClusterReadModel>,
) {
    fun asList(): List<ClusterReadModel> = items

    fun isEmpty(): Boolean = items.isEmpty()

    fun map(transform: (ClusterReadModel) -> ClusterReadModel): Clusters = of(items.map(transform))

    companion object {
        fun of(items: List<ClusterReadModel>): Clusters = Clusters(items.toList())

        fun empty(): Clusters = Clusters(emptyList())
    }
}

data class MapPhotoReadModel(
    val id: Long,
    val thumbnailUrl: String,
    val longitude: Double,
    val latitude: Double,
    val takenAt: LocalDateTime,
)

@JvmInline
value class MapPhotos private constructor(
    private val items: List<MapPhotoReadModel>,
) {
    fun asList(): List<MapPhotoReadModel> = items

    fun isEmpty(): Boolean = items.isEmpty()

    companion object {
        fun of(items: List<MapPhotoReadModel>): MapPhotos = MapPhotos(items.toList())

        fun empty(): MapPhotos = MapPhotos(emptyList())
    }
}

data class MapPhotosReadModel(
    val clusters: Clusters? = null,
    val photos: MapPhotos? = null,
)

data class ClusterPhotoReadModel(
    val id: Long,
    val url: String,
    val longitude: Double,
    val latitude: Double,
    val takenAt: LocalDateTime,
    val address: String,
)

@JvmInline
value class ClusterPhotos private constructor(
    private val items: List<ClusterPhotoReadModel>,
) {
    fun asList(): List<ClusterPhotoReadModel> = items

    fun isEmpty(): Boolean = items.isEmpty()

    companion object {
        fun of(items: List<ClusterPhotoReadModel>): ClusterPhotos = ClusterPhotos(items.toList())

        fun empty(): ClusterPhotos = ClusterPhotos(emptyList())
    }
}

data class BoundingBoxReadModel(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
)

data class AlbumMapInfoReadModel(
    val albumId: Long,
    val centerLongitude: Double?,
    val centerLatitude: Double?,
    val boundingBox: BoundingBoxReadModel?,
)

data class LocationInfoReadModel(
    val address: String?,
    val roadName: String? = null,
    val placeName: String?,
    val regionName: String?,
)

data class PlaceReadModel(
    val placeName: String,
    val address: String?,
    val roadAddress: String?,
    val longitude: Double,
    val latitude: Double,
    val category: String?,
)

@JvmInline
value class Places private constructor(
    private val items: List<PlaceReadModel>,
) {
    fun asList(): List<PlaceReadModel> = items

    fun isEmpty(): Boolean = items.isEmpty()

    companion object {
        fun of(items: List<PlaceReadModel>): Places = Places(items.toList())

        fun empty(): Places = Places(emptyList())
    }
}

data class PlaceSearchReadModel(
    val places: Places,
)

@JvmInline
value class ThumbnailUrls private constructor(
    private val items: List<String>,
) {
    fun asList(): List<String> = items

    companion object {
        fun of(items: List<String>): ThumbnailUrls = ThumbnailUrls(items.toList())
    }
}

data class AlbumThumbnailsReadModel(
    val id: Long,
    val title: String,
    val photoCount: Int,
    val thumbnailUrls: ThumbnailUrls,
)

@JvmInline
value class AlbumThumbnails private constructor(
    private val items: List<AlbumThumbnailsReadModel>,
) {
    fun asList(): List<AlbumThumbnailsReadModel> = items

    fun isEmpty(): Boolean = items.isEmpty()

    companion object {
        fun of(items: List<AlbumThumbnailsReadModel>): AlbumThumbnails = AlbumThumbnails(items.toList())

        fun empty(): AlbumThumbnails = AlbumThumbnails(emptyList())
    }
}

data class MapMeReadModel(
    val location: LocationInfoReadModel,
    val boundingBox: BoundingBoxReadModel,
    val totalHistoryCount: Int,
    val albums: AlbumThumbnails,
    val dataVersion: Long,
    val clusters: Clusters?,
    val photos: MapPhotos?,
)
