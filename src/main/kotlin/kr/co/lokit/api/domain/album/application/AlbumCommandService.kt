package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.annotation.OptimisticRetry
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.evictKey
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
) : CreateAlbumUseCase,
    UpdateAlbumUseCase {
    @OptimisticRetry
    @Transactional
    override fun create(
        album: Album,
        userId: Long,
    ): Album {
        val coupleId =
            albumRepository.findDefaultByUserId(userId)?.coupleId
                ?: throw BusinessException.DefaultAlbumNotFoundForUserException(
                    errors = errorDetailsOf(ErrorField.USER_ID to userId),
                )

        if (albumRepository.existsByCoupleIdAndTitle(coupleId, album.title)) {
            throw BusinessException.AlbumAlreadyExistsException(
                errors = errorDetailsOf(ErrorField.TITLE to album.title),
            )
        }
        val saved = albumRepository.save(album, userId)
        cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, coupleId)
        return saved
    }

    @OptimisticRetry
    @Transactional
    override fun updateTitle(
        id: Long,
        title: String,
        userId: Long,
    ): Album {
        val album =
            albumRepository.findById(id)
                ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.DefaultAlbumTitleChangeNotAllowedException(
                errors = errorDetailsOf(ErrorField.ALBUM_ID to id),
            )
        }
        if (album.title != title && albumRepository.existsByCoupleIdAndTitle(album.coupleId, title)) {
            throw BusinessException.AlbumAlreadyExistsException(
                errors = errorDetailsOf(ErrorField.TITLE to title),
            )
        }
        val updated = albumRepository.update(album.copy(title = title))
        cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, album.coupleId)
        return updated
    }

    @OptimisticRetry
    @Transactional
    @Caching(
        evict = [
            CacheEvict(cacheNames = [CacheNames.ALBUM_COUPLE], key = "#id"),
            CacheEvict(cacheNames = [CacheNames.ALBUM], key = "#userId + ':' + #id"),
        ],
    )
    override fun delete(
        id: Long,
        userId: Long,
    ) {
        val album =
            albumRepository.findById(id)
                ?: throw entityNotFound<Album>(id)
        if (album.isDefault) {
            throw BusinessException.DefaultAlbumDeletionNotAllowedException(
                errors = errorDetailsOf(ErrorField.ALBUM_ID to id),
            )
        }
        albumRepository.deleteById(id)
        cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, album.coupleId)
        mapPhotosCacheService.evictForCouple(album.coupleId)
    }
}
