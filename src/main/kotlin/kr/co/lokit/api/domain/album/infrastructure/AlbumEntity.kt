package kr.co.lokit.api.domain.album.infrastructure

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import java.time.LocalDateTime

@Entity(name = "Album")
@Table(
    indexes = [
        Index(columnList = "photo_added_at, created_at"),
        Index(columnList = "couple_id"),
    ],
)
class AlbumEntity(
    @Column(nullable = false, length = 10)
    var title: String,
    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: CoupleEntity,
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: UserEntity,
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    val isDefault: Boolean = false,
) : BaseEntity() {
    @Column(nullable = false)
    var photoCount: Int = 0
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "album",
    )
    var photos: MutableList<PhotoEntity> = mutableListOf()
        protected set

    var photoAddedAt: LocalDateTime? = null
        protected set

    fun onPhotoAdded() {
        photoAddedAt = LocalDateTime.now()
        photoCount++
    }

    fun onPhotoRemoved() {
        if (photoCount > 0) {
            photoCount--
        }
    }

    fun updateTitle(title: String) {
        this.title = title
    }
}
