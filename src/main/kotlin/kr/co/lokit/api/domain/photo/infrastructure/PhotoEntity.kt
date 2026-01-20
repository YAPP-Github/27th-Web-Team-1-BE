package kr.co.lokit.api.domain.photo.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity

@Entity
class PhotoEntity(
    @Column(nullable = false, length = 2100)
    val url: String,
    @JoinColumn
    @ManyToOne(fetch = FetchType.LAZY)
    val album: AlbumEntity,
    @Column(nullable = false)
    val longitude: Double,
    @Column(nullable = false)
    val latitude: Double,
) : BaseEntity() {

    init {
        album.addPhoto(this)
    }

    @Column(length = 1000)
    var description: String? = null
}
