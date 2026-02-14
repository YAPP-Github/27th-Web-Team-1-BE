package kr.co.lokit.api.domain.album.application.port.`in`

import kr.co.lokit.api.domain.album.domain.Album

interface GetAlbumUseCase {
    fun getSelectableAlbums(userId: Long): List<Album>
}
