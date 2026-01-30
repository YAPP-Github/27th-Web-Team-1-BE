package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class AlbumRepositoryImpl(
    private val albumJpaRepository: AlbumJpaRepository,
    private val workspaceJpaRepository: WorkspaceJpaRepository,
) : AlbumRepository {

    @Transactional
    override fun save(album: Album): Album {
        val workspace = workspaceJpaRepository.findByIdOrNull(album.workspaceId)
            ?: throw entityNotFound<Workspace>(album.workspaceId)
        val albumEntity = album.toEntity(workspace)
        val savedEntity = albumJpaRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Album? =
        albumJpaRepository.findByIdOrNull(id)?.toDomain()

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: Long): List<Album> =
        albumJpaRepository.findAllByUserId(userId).map { it.toDomain() }

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
        return albumJpaRepository.findAllWithPhotos().map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findByIdWithPhotos(id: Long): List<Album> {
        return albumJpaRepository.findByIdWithPhotos(id).map { it.toDomain() }
    }
}
