package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.entity.BaseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_email", columnNames = ["email"])],
)
class UserEntity(
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false)
    val name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER,
) : BaseEntity(),
    UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(role.authority))

    override fun getPassword(): String? = null

    override fun getUsername(): String = email
}
