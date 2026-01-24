package kr.co.lokit.api.domain.album.dto

data class SelectableAlbumResponse(
    val albums: List<SelectableAlbum>,
) {
    data class SelectableAlbum(
        val id: Long,
        val title: String,
        val photoCount: Int,
        val thumbnailUrl: String?,
    )
}
