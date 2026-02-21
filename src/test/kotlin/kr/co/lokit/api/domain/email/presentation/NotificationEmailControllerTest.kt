package kr.co.lokit.api.domain.email.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.common.permission.PermissionService
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.email.application.port.`in`.SaveNotificationEmailUseCase
import kr.co.lokit.api.domain.email.dto.SaveNotificationEmailRequest
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(NotificationEmailController::class)
class NotificationEmailControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    val objectMapper: ObjectMapper = ObjectMapper()

    @MockitoBean
    lateinit var compositeAuthenticationResolver: CompositeAuthenticationResolver

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var cookieProperties: CookieProperties

    @MockitoBean
    lateinit var cookieGenerator: CookieGenerator

    @MockitoBean
    lateinit var permissionService: PermissionService

    @MockitoBean
    lateinit var saveNotificationEmailUseCase: SaveNotificationEmailUseCase

    @Test
    fun `유효한 이메일이면 204를 반환하고 저장한다`() {
        mockMvc.perform(
            post("/emails")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SaveNotificationEmailRequest(email = "user@example.com"))),
        )
            .andExpect(status().isNoContent)

        verify(saveNotificationEmailUseCase).save("user@example.com")
    }

    @Test
    fun `유효하지 않은 이메일 형식이면 400과 요청값 포함 메시지를 반환한다`() {
        val invalidEmail = "invalid-email"

        mockMvc.perform(
            post("/emails")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SaveNotificationEmailRequest(email = invalidEmail))),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data.errors.email").value(org.hamcrest.Matchers.containsString(invalidEmail)))

        verifyNoInteractions(saveNotificationEmailUseCase)
    }
}
