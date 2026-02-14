package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.map.mapping.toDomain
import kr.co.lokit.api.domain.map.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaAlbumBoundsRepository(
    private val jpaRepository: AlbumBoundsJpaRepository,
) : AlbumBoundsRepositoryPort {
    override fun save(bounds: AlbumBounds): AlbumBounds {
        val entity = bounds.toEntity()
        return jpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByStandardIdAndIdType(
        standardId: Long,
        idType: BoundsIdType,
    ): AlbumBounds? = jpaRepository.findByStandardIdAndIdType(standardId, idType)?.toDomain()

    @Transactional
    override fun update(bounds: AlbumBounds): AlbumBounds {
        val entity =
            jpaRepository.findByIdOrNull(bounds.id)
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
