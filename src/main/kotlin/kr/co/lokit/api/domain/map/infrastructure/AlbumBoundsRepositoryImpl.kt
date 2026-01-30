package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.mapping.toDomain
import kr.co.lokit.api.domain.map.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class AlbumBoundsRepositoryImpl(
    private val jpaRepository: AlbumBoundsJpaRepository,
) : AlbumBoundsRepository {

    override fun save(bounds: AlbumBounds): AlbumBounds {
        val entity = bounds.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    override fun findByAlbumIdOrNull(albumId: Long): AlbumBounds? =
        jpaRepository.findByStandardId(albumId)?.toDomain()

    override fun apply(bounds: AlbumBounds): AlbumBounds {
        val entity = jpaRepository.findByIdOrNull(bounds.id)
            ?: throw entityNotFound<AlbumBounds>(bounds.id)

        entity.apply {
            minLongitude = bounds.minLongitude
            maxLongitude = bounds.maxLongitude
            minLatitude = bounds.minLatitude
            maxLatitude = bounds.maxLatitude
        }
        return entity.toDomain()
    }
}
