package kr.co.lokit.api.domain.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.core.userdetails.UserDetails

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserDetails?

    fun existsByEmail(email: String): Boolean
}
