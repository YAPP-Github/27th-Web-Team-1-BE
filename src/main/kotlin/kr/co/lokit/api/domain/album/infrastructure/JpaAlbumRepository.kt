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
        val ids = albumJpaRepository.findAlbumIdsByUserId(userId)
        if (ids.isEmpty()) {
            return emptyList()
        }
        return findAllByIds(ids)
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
        return findAllByIds(ids)
    }

    @Transactional(readOnly = true)
    override fun findAllByIds(ids: List<Long>): List<Album> {
        if (ids.isEmpty()) return emptyList()

        val entityMap =
            albumJpaRepository.findAllWithPhotosByIds(ids).associateBy { it.nonNullId() }

        return ids.mapNotNull { entityMap[it]?.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(id: Long): List<Album> {
        return albumJpaRepository.findByIdWithPhotos(id).map { it.toDomain() }
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
