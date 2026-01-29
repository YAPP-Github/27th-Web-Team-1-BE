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
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity
import java.time.LocalDateTime

@Entity(name = "Album")
@Table(
    indexes = [
        Index(columnList = "photo_added_at, created_at"),
        Index(columnList = "workspace_id"),
        Index(columnList = "thumbnail_id")
    ],
)
class AlbumEntity(
    @Column(nullable = false, length = 10)
    var title: String,
    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    val workspace: WorkspaceEntity,
) : BaseEntity() {

    init {
        workspace.addAlbum(this)
    }

    @Column(nullable = false)
    var photoCount: Int = 0
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "album"
    )
    var photos: MutableList<PhotoEntity> = mutableListOf()
        protected set

    @JoinColumn(name = "thumbnail_id")
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

    fun updateTitle(title: String) {
        this.title = title
    }
}
