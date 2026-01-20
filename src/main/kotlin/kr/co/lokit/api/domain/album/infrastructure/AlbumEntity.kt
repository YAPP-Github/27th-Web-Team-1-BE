package kr.co.lokit.api.domain.album.infrastructure

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity

@Entity
@Table(indexes = [Index(columnList = "photoCount"), Index(columnList = "created_at")])
class AlbumEntity(
    @Column(nullable = false, length = 10)
    val title: String,
) : BaseEntity() {

    @Column(unique = true)
    var inviteCode: String? = null

    @Column(nullable = false)
    var photoCount: Int = 0
        get() = photos.size
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "album"
    )
    var photos = mutableListOf<PhotoEntity>()
        protected set

    @JoinColumn
    @OneToOne(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
    )
    var thumbnail: PhotoEntity? = null
        protected set

    @PrePersist
    @PreUpdate
    fun syncPhotoCount() {
        photoCount = photos.size
    }

    fun addPhoto(photo: PhotoEntity) {
        if (photos.isEmpty()) {
            thumbnail = photo
        }
        photos.add(photo)
        syncPhotoCount()
    }
}
