package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.application.port.`in`.CreateAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.UpdateAlbumUseCase
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.map.application.MapPhotosCacheService
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumCommandService(
    private val albumRepository: AlbumRepositoryPort,
    private val mapPhotosCacheService: MapPhotosCacheService,
    private val cacheManager: CacheManager,
) : CreateAlbumUseCase, UpdateAlbumUseCase {

    @OptimisticRetry
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
        val saved = albumRepository.save(album, userId)
        cacheManager.getCache("coupleAlbums")?.evict(coupleId)
        return saved
    }

    @OptimisticRetry
    @Transactional
    override fun updateTitle(id: Long, title: String, userId: Long): Album {
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
        val updated = albumRepository.applyTitle(id, title)
        cacheManager.getCache("coupleAlbums")?.evict(album.coupleId)
        return updated
    }

    @OptimisticRetry
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = ["albumCouple"], key = "#id"),
            CacheEvict(cacheNames = ["album"], key = "#userId + ':' + #id"),
        ],
    )
    override fun delete(id: Long, userId: Long) {
        val album = albumRepository.findById(id)
            ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.DefaultAlbumDeletionNotAllowedException(
                errors = mapOf("albumId" to id.toString()),
            )
        }
        albumRepository.deleteById(id)
        cacheManager.getCache("coupleAlbums")?.evict(album.coupleId)
        mapPhotosCacheService.evictForCouple(album.coupleId)
    }
}
