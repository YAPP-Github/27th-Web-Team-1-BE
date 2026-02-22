package kr.co.lokit.api.domain.couple.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.couple.domain.InviteCodeStatus
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import java.time.LocalDateTime

@Entity(name = "InviteCode")
class InviteCodeEntity(
    @Column(nullable = false, length = 6)
    val code: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: UserEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: InviteCodeStatus = InviteCodeStatus.UNUSED,
    @Column(nullable = false)
    val expiresAt: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    var usedBy: UserEntity? = null,
    var usedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean = expiresAt.isBefore(now) || expiresAt.isEqual(now)

    fun revoke() {
        status = InviteCodeStatus.REVOKED
    }

    fun expire() {
        status = InviteCodeStatus.EXPIRED
    }

    fun useBy(
        user: UserEntity,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        status = InviteCodeStatus.USED
        usedBy = user
        usedAt = now
    }
}
