package kr.co.lokit.api.domain.photo.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

@Entity
@Table(
    name = "photo",
    indexes = [
        Index(name = "idx_photo_location", columnList = "location"),
    ],
)
class PhotoEntity(
    @Column(nullable = false, length = 2100)
    val url: String,
    @JoinColumn(nullable = false)
    @ManyToOne
    val album: AlbumEntity,
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    val location: Point,
) : BaseEntity() {
    val longitude: Double
        get() = location.x

    val latitude: Double
        get() = location.y

    init {
        album.addPhoto(this)
    }

    @Column(length = 1000)
    var description: String? = null

    companion object {
        private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

        fun createPoint(
            longitude: Double,
            latitude: Double,
        ): Point = geometryFactory.createPoint(Coordinate(longitude, latitude))
    }
}
