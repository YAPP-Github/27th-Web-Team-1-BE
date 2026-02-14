package kr.co.lokit.api.domain.couple.infrastructure

import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.hibernate.annotations.Immutable

@Immutable
@Entity(name = "CoupleUser")
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["couple_id", "user_id"])],
    indexes = [Index(columnList = "couple_id"), Index(columnList = "user_id")],
)
class CoupleUserEntity(
    @ManyToOne
    @JoinColumn(name = "couple_id", nullable = false)
    val couple: CoupleEntity,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
) : BaseEntity()
