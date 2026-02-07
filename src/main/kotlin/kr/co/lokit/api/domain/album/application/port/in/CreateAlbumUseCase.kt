package kr.co.lokit.api.domain.album.application.port.`in`

import kr.co.lokit.api.domain.album.domain.Album

interface CreateAlbumUseCase {
    fun create(album: Album, userId: Long): Album
}
