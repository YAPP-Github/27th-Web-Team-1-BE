package kr.co.lokit.api.domain.photo.domain

import kr.co.lokit.api.domain.album.domain.Album

data class Photo(
    val id: Long,
    val url: String,
    val album: Album,
    val location: Location
) {
    var description: String? = null
}
