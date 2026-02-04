package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.application.port.`in`.GetAlbumUseCase
import kr.co.lokit.api.domain.album.domain.Album
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AlbumQueryService(
    private val albumRepository: AlbumRepositoryPort,
) : GetAlbumUseCase {

    override fun getSelectableAlbums(userId: Long): List<Album> =
        albumRepository.findAllByUserId(userId).filter { !it.isDefault }
}
