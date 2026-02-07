package kr.co.lokit.api.domain.album.application.port.`in`

import kr.co.lokit.api.domain.album.domain.Album

interface UpdateAlbumUseCase {
    fun updateTitle(id: Long, title: String, userId: Long): Album
    fun delete(id: Long, userId: Long)
}
