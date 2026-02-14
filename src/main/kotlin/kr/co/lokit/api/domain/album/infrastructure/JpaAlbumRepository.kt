package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.common.util.orZero
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaAlbumRepository(
    private val albumJpaRepository: AlbumJpaRepository,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : AlbumRepositoryPort {
    private fun enrichDefaultAlbumWithAllPhotos(
        album: Album,
        coupleId: Long,
    ): Album {
        if (!album.isDefault) {
            return album
        }

        val allAlbums =
            findAllByCoupleIdInternal(coupleId)
                .filter { it.id != album.id }

        val allPhotos =
            (album.photos + allAlbums.flatMap { it.photos })
                .distinctBy { it.id }

        val actualPhotoCount = allPhotos.size

        return album
            .copy(
                photoCount = actualPhotoCount,
            ).apply {
                this.photos = allPhotos
            }
    }

    private fun findAllByUserIdInternal(userId: Long): List<Album> {
        val ids = albumJpaRepository.findAlbumIdsByUserId(userId)
        if (ids.isEmpty()) {
            return emptyList()
        }
        val entityMap =
            albumJpaRepository.findAllWithPhotosByIds(ids).associateBy { it.nonNullId() }

        return ids.mapNotNull { entityMap[it]?.toDomain() }
    }

    private fun findAllByCoupleIdInternal(coupleId: Long): List<Album> {
        val ids = albumJpaRepository.findAlbumIdsByCoupleId(coupleId)
        if (ids.isEmpty()) {
            return emptyList()
        }
        val entityMap =
            albumJpaRepository.findAllWithPhotosByIds(ids).associateBy { it.nonNullId() }

        return ids.mapNotNull { entityMap[it]?.toDomain() }
    }

    @Transactional
    override fun save(
        album: Album,
        userId: Long,
    ): Album {
        val couple =
            coupleJpaRepository.findByUserId(userId)
                ?: throw entityNotFound<Couple>(userId)
        val userEntity =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw entityNotFound<User>(userId)
        val albumEntity = album.toEntity(couple, userEntity)
        val savedEntity = albumJpaRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Album? = albumJpaRepository.findByIdOrNull(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: Long): List<Album> {
        val albums = findAllByUserIdInternal(userId)

        return albums.map { album ->
            if (album.isDefault) {
                enrichDefaultAlbumWithAllPhotos(album, album.coupleId)
            } else {
                album
            }
        }
    }

    @Cacheable(cacheNames = [CacheNames.COUPLE_ALBUMS], key = "#coupleId", sync = true)
    @Transactional(readOnly = true)
    override fun findAllByCoupleId(coupleId: Long): List<Album> {
        val albums = findAllByCoupleIdInternal(coupleId)

        return albums.map { album ->
            if (album.isDefault) {
                enrichDefaultAlbumWithAllPhotos(album, coupleId)
            } else {
                album
            }
        }
    }

    override fun update(album: Album): Album {
        val albumEntity =
            albumJpaRepository.findByIdOrNull(album.id)
                ?: throw entityNotFound<Album>(album.id)
        albumEntity.updateTitle(album.title)
        return albumEntity.toDomain()
    }

    @Transactional
    override fun deleteById(id: Long) {
        albumJpaRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun findAllWithPhotos(): List<Album> {
        val ids = albumJpaRepository.findAllAlbumIds()
        if (ids.isEmpty()) return emptyList()
        return findAllByIds(ids)
    }

    @Transactional(readOnly = true)
    override fun findAllByIds(ids: List<Long>): List<Album> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val entityMap =
            albumJpaRepository.findAllWithPhotosByIds(ids).associateBy { it.nonNullId() }

        val albums = ids.mapNotNull { entityMap[it]?.toDomain() }

        return albums.map { album ->
            if (album.isDefault) {
                enrichDefaultAlbumWithAllPhotos(album, album.coupleId)
            } else {
                album
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(id: Long): List<Album> = findByIdWithPhotos(id, null)

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(
        id: Long,
        @Suppress("UNUSED_PARAMETER") userId: Long?,
    ): List<Album> {
        val albums = albumJpaRepository.findByIdWithPhotos(id).map { it.toDomain() }

        return albums.map { album ->
            if (album.isDefault) {
                enrichDefaultAlbumWithAllPhotos(album, album.coupleId)
            } else {
                album
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findDefaultByUserId(userId: Long): Album? =
        albumJpaRepository.findByUserIdAndIsDefaultTrue(userId)?.toDomain()

    @Transactional(readOnly = true)
    override fun existsByCoupleIdAndTitle(
        coupleId: Long,
        title: String,
    ): Boolean = albumJpaRepository.existsByCoupleIdAndTitle(coupleId, title)

    @Transactional(readOnly = true)
    override fun photoCountSumByUserId(userId: Long): Int = albumJpaRepository.sumPhotoCountByUserId(userId).orZero()
}
