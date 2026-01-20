package kr.co.lokit.api.domain.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.domain.UserRole
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
    private val email: String,
    @Column(nullable = false)
    private val name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,
) : BaseEntity(),
    UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String? = null

    override fun getUsername(): String = email

    fun getName(): String = name

    fun toDomain(): User =
        User(
            id = id,
            email = email,
            name = name,
            role = UserRole.valueOf(role.name),
        )

    companion object {
        fun from(user: User): UserEntity =
            UserEntity(
                email = user.email,
                name = user.name,
                role = Role.valueOf(user.role.name),
            )
    }
}

enum class Role(
    val authority: String,
) {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
}
