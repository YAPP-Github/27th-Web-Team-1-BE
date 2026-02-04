package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.application.port.`in`.CreateAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.UpdateAlbumUseCase
import kr.co.lokit.api.domain.album.domain.Album
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumCommandService(
    private val albumRepository: AlbumRepositoryPort,
) : CreateAlbumUseCase, UpdateAlbumUseCase {

    @Transactional
    override fun create(album: Album, userId: Long): Album {
        val coupleId = albumRepository.findDefaultByUserId(userId)?.coupleId
            ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                errors = mapOf("userId" to userId.toString())
            )

        if (albumRepository.existsByCoupleIdAndTitle(coupleId, album.title)) {
            throw BusinessException.AlbumAlreadyExistsException(
                errors = mapOf("title" to album.title)
            )
        }
        return albumRepository.save(album, userId)
    }

    @Transactional
    override fun updateTitle(id: Long, title: String): Album {
        val album = albumRepository.findById(id)
            ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.DefaultAlbumTitleChangeNotAllowedException(
                errors = mapOf("albumId" to id.toString()),
            )
        }
        if (album.title != title && albumRepository.existsByCoupleIdAndTitle(album.coupleId, title)) {
            throw BusinessException.AlbumAlreadyExistsException(
                errors = mapOf("title" to title)
            )
        }
        return albumRepository.applyTitle(id, title)
    }

    @Transactional
    override fun delete(id: Long) {
        val album = albumRepository.findById(id)
            ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.DefaultAlbumDeletionNotAllowedException(
                errors = mapOf("albumId" to id.toString()),
            )
        }
        albumRepository.deleteById(id)
    }
}
