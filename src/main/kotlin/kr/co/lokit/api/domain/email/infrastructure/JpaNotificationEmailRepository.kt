package kr.co.lokit.api.domain.email.infrastructure

import kr.co.lokit.api.domain.email.application.port.NotificationEmailRepositoryPort
import kr.co.lokit.api.domain.email.domain.NotificationEmail
import org.springframework.stereotype.Repository

@Repository
class JpaNotificationEmailRepository(
    private val notificationEmailJpaRepository: NotificationEmailJpaRepository,
) : NotificationEmailRepositoryPort {
    override fun existsByEmail(email: String): Boolean = notificationEmailJpaRepository.existsByEmail(email)

    override fun save(notificationEmail: NotificationEmail): NotificationEmail =
        notificationEmailJpaRepository.save(NotificationEmailEntity(email = notificationEmail.email)).let {
            NotificationEmail(email = it.email)
        }
}
