package kr.co.lokit.api.domain.map.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.map.domain.BoundsIdType

@Entity(name = "album_bounds")
@Table(
    indexes = [Index(columnList = "standard_id, id_type")],
)
class AlbumBoundsEntity(
    @Column(name = "standard_id")
    val standardId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false)
    val idType: BoundsIdType,
    @Column(name = "min_longitude", nullable = false)
    var minLongitude: Double,
    @Column(name = "max_longitude", nullable = false)
    var maxLongitude: Double,
    @Column(name = "min_latitude", nullable = false)
    var minLatitude: Double,
    @Column(name = "max_latitude", nullable = false)
    var maxLatitude: Double,
) : BaseEntity()
