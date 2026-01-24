package kr.co.lokit.api.domain.album.infrastructure

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.workspace.infrastructure.WorkSpaceEntity
import java.time.LocalDateTime

@Entity
@Table(indexes = [Index(columnList = "photo_added_at, created_at")])
class AlbumEntity(
    @Column(nullable = false, length = 10)
    val title: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val workspace: WorkSpaceEntity,
) : BaseEntity() {
    @Column(nullable = false)
    var photoCount: Int = 0
        get() = photos.size
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "album"
    )
    var photos: MutableList<PhotoEntity> = mutableListOf()
        protected set

    @JoinColumn
    @OneToOne(
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
    )
    var thumbnail: PhotoEntity? = null
        protected set

    var photoAddedAt: LocalDateTime? = null
        protected set

    fun addPhoto(photo: PhotoEntity) {
        if (photos.contains(photo)) {
            return
        }
        if (photos.isEmpty()) {
            thumbnail = photo
        }
        photos.add(photo)
        photoAddedAt = LocalDateTime.now()
        photoCount++
    }
}
