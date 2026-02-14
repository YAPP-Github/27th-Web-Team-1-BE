package kr.co.lokit.api.domain.photo.domain

import java.time.LocalDateTime

data class Photo(
    val id: Long = 0L,
    val albumId: Long?,
    val coupleId: Long? = null,
    val location: Location,
    val description: String? = null,
    var url: String,
    var uploadedById: Long,
    var takenAt: LocalDateTime = LocalDateTime.now(),
    val address: String? = null,
) {
    fun hasLocation(): Boolean = location.longitude != 0.0 && location.latitude != 0.0

    fun withAddress(address: String?): Photo = copy(address = address)

    fun withDefaultAlbum(defaultAlbumId: Long?): Photo =
        if (defaultAlbumId != null) {
            copy(albumId = defaultAlbumId)
        } else {
            this
        }

    fun update(
        albumId: Long?,
        description: String?,
        longitude: Double?,
        latitude: Double?,
    ): Photo =
        copy(
            albumId = albumId ?: this.albumId,
            description = description ?: this.description,
            location =
                location.copy(
                    longitude = longitude ?: location.longitude,
                    latitude = latitude ?: location.latitude,
                ),
        )
}
