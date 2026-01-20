package kr.co.lokit.api.photo.domain

import kr.co.lokit.api.album.domain.Album

data class Photo(
    val id: Long,
    val url: String,
    val album: Album,
    val longitude: Double,
    val latitude: Double,
) {
    var description: String? = null
}
