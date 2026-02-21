package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kr.co.lokit.api.common.constants.AccountStatus
import kr.co.lokit.api.common.constants.UserRole
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.domain.User
import org.hibernate.annotations.NaturalId
import java.time.LocalDateTime

@Entity(name = "Users")
class UserEntity(
    @NaturalId
    @Column(nullable = false, length = 320)
    var email: String,
    @Column(nullable = false, length = 30)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
    @Column(length = 2100)
    var profileImageUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AccountStatus = AccountStatus.ACTIVE,
    var withdrawnAt: LocalDateTime? = null,
) : BaseEntity() {
    fun apply(user: User) {
        name = user.name
        profileImageUrl = user.profileImageUrl
    }

    fun markWithdrawn(now: LocalDateTime) {
        status = AccountStatus.WITHDRAWN
        withdrawnAt = now
    }

    fun reactivate() {
        status = AccountStatus.ACTIVE
        withdrawnAt = null
    }
}
