package kr.co.lokit.api.domain.photo.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDateTime

@Entity(name = "Photo")
@Table(
    indexes = [
        Index(columnList = "album_id"),
        Index(columnList = "uploaded_by"),
        Index(name = "idx_photo_location", columnList = "location"),
    ],
)
class PhotoEntity(
    @Column(nullable = false, unique = true, length = 2100)
    var url: String,
    @Column(name = "taken_at", nullable = false)
    var takenAt: LocalDateTime = LocalDateTime.now(),
    @JoinColumn(name = "album_id", nullable = false)
    @ManyToOne
    var album: AlbumEntity,
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    var location: Point,
    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    var uploadedBy: UserEntity,
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

    fun updateLocation(longitude: Double, latitude: Double) {
        this.location = createPoint(longitude, latitude)
    }

    companion object {
        private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

        fun createPoint(
            longitude: Double,
            latitude: Double,
        ): Point = geometryFactory.createPoint(Coordinate(longitude, latitude))
    }
}
