package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.workspace.domain.WorkSpace
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AlbumRepositoryImpl(
    private val albumRepository: AlbumJpaRepository,
    private val workspaceJpaRepository: WorkspaceJpaRepository,
) : AlbumRepository {

    override fun save(album: Album): Album {
        val workspace = workspaceJpaRepository.findByIdOrNull(album.workspaceId)
            ?: throw entityNotFound<WorkSpace>(album.workspaceId)
        val albumEntity = album.toEntity(workspace)
        val savedEntity = albumRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    override fun findAllByUserId(userId: Long): List<Album> {
        return albumRepository.findAllByUserId(userId).map { it.toDomain() }
    }
}
