package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import java.time.LocalDateTime

@Entity(name = "RefreshToken")
@Table(
    indexes = [Index(columnList = "user_id")]
)
class RefreshTokenEntity(
    @Column(nullable = false)
    val token: String,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
) : BaseEntity()
