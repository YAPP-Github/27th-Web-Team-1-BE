package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.album.mapping.toDomain
import kr.co.lokit.api.domain.album.mapping.toEntity
import org.springframework.stereotype.Repository

@Repository
class AlbumRepositoryImpl(
    private val albumRepository: AlbumJpaRepository
) : AlbumRepository {

    override fun save(album: Album): Album {
        val albumEntity = album.toEntity()
        val savedEntity = albumRepository.save(albumEntity)
        return savedEntity.toDomain()
    }

    override fun findAllByUserId(userId: Long): List<Album> {
        return albumRepository.findAllByUserId(userId).map { it.toDomain() }
    }
}
