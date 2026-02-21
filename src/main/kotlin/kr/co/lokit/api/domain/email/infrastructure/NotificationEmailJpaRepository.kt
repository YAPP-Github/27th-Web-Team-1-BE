package kr.co.lokit.api.domain.email.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface NotificationEmailJpaRepository : JpaRepository<NotificationEmailEntity, Long> {
    fun existsByEmail(email: String): Boolean
}
