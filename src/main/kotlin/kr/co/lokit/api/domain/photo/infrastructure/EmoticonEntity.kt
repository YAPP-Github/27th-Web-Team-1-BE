package kr.co.lokit.api.domain.photo.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity

@Entity(name = "Emoticon")
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["comment_id", "user_id", "emoji"])],
    indexes = [Index(columnList = "comment_id")],
)
class EmoticonEntity(
    @ManyToOne
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: CommentEntity,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false, length = 4)
    val emoji: String,
) : BaseEntity()
