package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.co.lokit.api.common.entity.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token")
class RefreshTokenEntity(
    @Column(nullable = false, unique = true)
    val token: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
) : BaseEntity()
