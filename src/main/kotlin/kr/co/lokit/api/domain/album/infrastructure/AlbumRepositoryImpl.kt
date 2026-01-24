package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import kr.co.lokit.api.domain.workspace.domain.WorkSpace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AlbumRepositoryImpl(
    private val albumJpaRepository: AlbumJpaRepository,
    private val workspaceJpaRepository: WorkspaceJpaRepository,
) : AlbumRepository {

    override fun save(album: Album): Album {
        val workspace = workspaceJpaRepository.findByIdOrNull(album.workspaceId)
            ?: throw entityNotFound<WorkSpace>(album.workspaceId)
        val albumEntity = album.toEntity(workspace)
        val savedEntity = albumJpaRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    override fun findById(id: Long): Album? =
        albumJpaRepository.findByIdOrNull(id)?.toDomain()

    override fun findAllByUserId(userId: Long): List<Album> =
        albumJpaRepository.findAllByUserId(userId).map { it.toDomain() }

    override fun updateTitle(id: Long, title: String): Album {
        val albumEntity = albumJpaRepository.findByIdOrNull(id)
            ?: throw entityNotFound<Album>(id)
        albumEntity.updateTitle(title)
        return albumEntity.toDomain()
    }

    override fun deleteById(id: Long) {
        val albumEntity = albumJpaRepository.findByIdOrNull(id)
            ?: throw entityNotFound<Album>(id)
        albumJpaRepository.delete(albumEntity)
    }

    override fun findAllWithPhotos(): List<AlbumEntity> {
        return albumJpaRepository.findAllWithPhotos()
    }
}
