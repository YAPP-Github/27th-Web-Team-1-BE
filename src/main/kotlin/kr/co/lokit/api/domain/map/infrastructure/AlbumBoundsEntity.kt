package kr.co.lokit.api.domain.map.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity

@Entity(name = "album_bounds")
@Table(
    indexes = [Index(columnList = "album_id")],
)
class AlbumBoundsEntity(
    @Column(name = "album_id", nullable = false)
    val albumId: Long,
    @Column(name = "min_longitude", nullable = false)
    var minLongitude: Double,
    @Column(name = "max_longitude", nullable = false)
    var maxLongitude: Double,
    @Column(name = "min_latitude", nullable = false)
    var minLatitude: Double,
    @Column(name = "max_latitude", nullable = false)
    var maxLatitude: Double,
) : BaseEntity()
