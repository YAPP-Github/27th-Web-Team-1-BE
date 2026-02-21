package kr.co.lokit.api.domain.email.application.port

import kr.co.lokit.api.domain.email.domain.NotificationEmail

interface NotificationEmailRepositoryPort {
    fun existsByEmail(email: String): Boolean

    fun save(notificationEmail: NotificationEmail): NotificationEmail
}
