package kr.co.lokit.api.domain.map.infrastructure

import jakarta.persistence.EntityNotFoundException
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.mapping.toDomain
import kr.co.lokit.api.domain.map.mapping.toEntity
import org.springframework.stereotype.Repository

@Repository
class AlbumBoundsRepositoryImpl(
    private val jpaRepository: AlbumBoundsJpaRepository,
) : AlbumBoundsRepository {

    override fun save(bounds: AlbumBounds): AlbumBounds {
        val entity = bounds.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByAlbumId(albumId: Long): AlbumBounds? =
        jpaRepository.findByAlbumId(albumId)?.toDomain()

    override fun updateBounds(bounds: AlbumBounds): AlbumBounds {
        val entity = jpaRepository.findByAlbumId(bounds.albumId)
            ?: throw EntityNotFoundException("AlbumBounds not found for albumId: ${bounds.albumId}")
        entity.minLongitude = bounds.minLongitude
        entity.maxLongitude = bounds.maxLongitude
        entity.minLatitude = bounds.minLatitude
        entity.maxLatitude = bounds.maxLatitude
        return entity.toDomain()
    }
}
