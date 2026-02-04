package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaAlbumRepository(
    private val albumJpaRepository: AlbumJpaRepository,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : AlbumRepositoryPort {

    /**
     * default 앨범인 경우 couple 내의 다른 모든 앨범의 사진들을 합쳐서 반환
     */
    private fun enrichDefaultAlbumWithAllPhotos(album: Album, userId: Long): Album {
        if (!album.isDefault) {
            return album
        }

        val allAlbums = findAllByUserIdInternal(userId)
            .filter { !it.isDefault && it.id != album.id }

        val allPhotos = allAlbums.flatMap { it.photos }

        return album.copy(
            photoCount = allPhotos.size
        ).apply {
            this.photos = allPhotos
        }
    }

    /**
     * userId를 통해 couple 내의 모든 앨범 조회 (내부용, default 앨범 처리 없음)
     */
    private fun findAllByUserIdInternal(userId: Long): List<Album> {
        val ids = albumJpaRepository.findAlbumIdsByUserId(userId)
        if (ids.isEmpty()) {
            return emptyList()
        }
        val entityMap =
            albumJpaRepository.findAllWithPhotosByIds(ids).associateBy { it.nonNullId() }

        return ids.mapNotNull { entityMap[it]?.toDomain() }
    }

    @Transactional
    override fun save(album: Album, userId: Long): Album {
        val couple = coupleJpaRepository.findByUserId(userId)
            ?: throw entityNotFound<Couple>(userId)
        val userEntity = userJpaRepository.findByIdOrNull(userId)
            ?: throw entityNotFound<User>(userId)
        val albumEntity = album.toEntity(couple, userEntity)
        val savedEntity = albumJpaRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Album? =
        albumJpaRepository.findByIdOrNull(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: Long): List<Album> {
        val albums = findAllByUserIdInternal(userId)

        return albums.map { album ->
            if (album.isDefault) {
                enrichDefaultAlbumWithAllPhotos(album, userId)
            } else {
                album
            }
        }
    }

    override fun applyTitle(id: Long, title: String): Album {
        val albumEntity = albumJpaRepository.findByIdOrNull(id)
            ?: throw entityNotFound<Album>(id)
        albumEntity.updateTitle(title)
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
        // findAllByIds에서 이미 default 앨범 처리가 됨
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

        // default 앨범이 있으면 처리 (coupleId를 통해 userId 찾기)
        return albums.map { album ->
            if (album.isDefault) {
                // coupleId를 통해 userId 찾기 (couple의 첫 번째 사용자 사용)
                val couple = coupleJpaRepository.findByIdFetchUsers(album.coupleId)
                    ?: return@map album
                val userId = couple.coupleUsers.firstOrNull()?.user?.nonNullId()
                    ?: return@map album
                enrichDefaultAlbumWithAllPhotos(album, userId)
            } else {
                album
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(id: Long): List<Album> {
        return findByIdWithPhotos(id, null)
    }

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(id: Long, userId: Long?): List<Album> {
        val albums = albumJpaRepository.findByIdWithPhotos(id).map { it.toDomain() }

        // default 앨범이 있으면 처리
        return albums.map { album ->
            if (album.isDefault) {
                val effectiveUserId = userId ?: run {
                    // userId가 없으면 coupleId를 통해 userId 찾기
                    val couple = coupleJpaRepository.findByIdFetchUsers(album.coupleId)
                        ?: return@map album
                    couple.coupleUsers.firstOrNull()?.user?.nonNullId()
                        ?: return@map album
                }
                enrichDefaultAlbumWithAllPhotos(album, effectiveUserId)
            } else {
                album
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findDefaultByUserId(userId: Long): Album? {
        val couple = coupleJpaRepository.findByUserId(userId) ?: return null
        return albumJpaRepository.findByCoupleIdAndIsDefaultTrue(couple.nonNullId())?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun existsByCoupleIdAndTitle(coupleId: Long, title: String): Boolean {
        return albumJpaRepository.existsByCoupleIdAndTitle(coupleId, title)
    }
}
