package kr.co.lokit.api.domain.email.application

import kr.co.lokit.api.domain.email.application.port.NotificationEmailRepositoryPort
import kr.co.lokit.api.domain.email.domain.NotificationEmail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class NotificationEmailServiceTest {
    @Mock
    lateinit var notificationEmailRepository: NotificationEmailRepositoryPort

    @InjectMocks
    lateinit var notificationEmailService: NotificationEmailService

    @Test
    fun `중복 이메일이면 저장하지 않는다`() {
        `when`(notificationEmailRepository.existsByEmail("user@example.com")).thenReturn(true)

        notificationEmailService.save(" user@example.com ")

        verify(notificationEmailRepository).existsByEmail("user@example.com")
        verifyNoMoreInteractions(notificationEmailRepository)
    }

    @Test
    fun `신규 이메일이면 정규화 후 저장한다`() {
        `when`(notificationEmailRepository.existsByEmail("user@example.com")).thenReturn(false)

        notificationEmailService.save(" User@Example.com ")

        verify(notificationEmailRepository).save(NotificationEmail(email = "user@example.com"))
    }
}
