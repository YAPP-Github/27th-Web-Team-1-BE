package kr.co.lokit.api.config.security

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserPrincipal(
    val id: Long,
    private val email: String,
    val name: String,
    val role: UserRole,
    val status: AccountStatus = AccountStatus.ACTIVE,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(role.authority))

    override fun getPassword(): String? = null

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = status == AccountStatus.ACTIVE

    companion object {
        fun from(entity: UserEntity): UserPrincipal =
            UserPrincipal(
                id = entity.nonNullId(),
                email = entity.email,
                name = entity.name,
                role = entity.role,
                status = entity.status,
            )
    }
}
