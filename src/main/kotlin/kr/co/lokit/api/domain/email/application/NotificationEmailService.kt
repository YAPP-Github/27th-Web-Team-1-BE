package kr.co.lokit.api.domain.email.application

import kr.co.lokit.api.domain.email.application.port.NotificationEmailRepositoryPort
import kr.co.lokit.api.domain.email.application.port.`in`.SaveNotificationEmailUseCase
import kr.co.lokit.api.domain.email.domain.NotificationEmail
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationEmailService(
    private val notificationEmailRepository: NotificationEmailRepositoryPort,
) : SaveNotificationEmailUseCase {
    @Transactional
    override fun save(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (notificationEmailRepository.existsByEmail(normalizedEmail)) {
            return
        }
        notificationEmailRepository.save(NotificationEmail(email = normalizedEmail))
    }
}
