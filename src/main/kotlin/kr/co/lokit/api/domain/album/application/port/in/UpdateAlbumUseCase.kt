package kr.co.lokit.api.domain.album.application.port.`in`

import kr.co.lokit.api.domain.album.domain.Album

interface UpdateAlbumUseCase {
    fun updateTitle(id: Long, title: String): Album
    fun delete(id: Long)
}
