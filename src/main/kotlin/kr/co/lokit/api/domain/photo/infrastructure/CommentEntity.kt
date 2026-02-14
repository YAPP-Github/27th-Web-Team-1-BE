package kr.co.lokit.api.domain.photo.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import java.time.LocalDate

@Entity(name = "Comment")
@Table(
    indexes = [
        Index(columnList = "photo_id"),
        Index(columnList = "user_id"),
    ],
)
class CommentEntity(
    @ManyToOne
    @JoinColumn(name = "photo_id", nullable = false)
    val photo: PhotoEntity,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false, length = 200)
    var content: String,
    @Column(nullable = false)
    var commentedAt: LocalDate,
) : BaseEntity()
