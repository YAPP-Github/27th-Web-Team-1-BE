package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.entity.BaseEntity
import org.hibernate.annotations.NaturalId
import java.time.LocalDateTime

@Entity(name = "Users")
class UserEntity(
    @NaturalId
    @Column(nullable = false)
    var email: String,
    @Column(nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    var profileImageUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AccountStatus = AccountStatus.ACTIVE,
    var withdrawnAt: LocalDateTime? = null,
) : BaseEntity()
