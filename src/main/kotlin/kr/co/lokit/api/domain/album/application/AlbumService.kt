package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumService(
    private val albumRepository: AlbumRepository,
) {
    @Transactional
    fun create(album: Album, userId: Long): Album =
        albumRepository.save(album, userId)

    @Transactional(readOnly = true)
    fun getSelectableAlbums(userId: Long): List<Album> =
        albumRepository.findAllByUserId(userId)

    @Transactional
    fun updateTitle(id: Long, title: String): Album {
        val album = albumRepository.findById(id)
            ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.BusinessRuleViolationException("기본 앨범의 이름은 변경할 수 없습니다.")
        }
        return albumRepository.applyTitle(id, title)
    }

    @Transactional
    fun delete(id: Long) {
        val album = albumRepository.findById(id)
            ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.BusinessRuleViolationException("기본 앨범은 삭제할 수 없습니다.")
        }
        albumRepository.deleteById(id)
    }
}
