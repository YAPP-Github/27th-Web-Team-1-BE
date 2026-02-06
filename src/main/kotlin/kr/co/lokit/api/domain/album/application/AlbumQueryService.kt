package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.application.port.`in`.GetAlbumUseCase
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AlbumQueryService(
    private val albumRepository: AlbumRepositoryPort,
    private val coupleRepository: CoupleRepositoryPort,
) : GetAlbumUseCase {
    override fun getSelectableAlbums(userId: Long): List<Album> {
        val coupleId = coupleRepository.findByUserId(userId)?.id ?: return emptyList()
        return albumRepository.findAllByCoupleId(coupleId).filter { !it.isDefault }
    }
}
