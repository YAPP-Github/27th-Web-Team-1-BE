package kr.co.lokit.api.domain.user.presentation

import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.port.`in`.WithdrawUseCase
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.ResponseCookie
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
class UserControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

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
    lateinit var withdrawUseCase: WithdrawUseCase

    @Test
    fun `회원 탈퇴 성공`() {
        whenever(cookieGenerator.clearAccessTokenCookie(any())).thenReturn(
            ResponseCookie.from("accessToken", "").maxAge(0).build(),
        )
        whenever(cookieGenerator.clearRefreshTokenCookie(any())).thenReturn(
            ResponseCookie.from("refreshToken", "").maxAge(0).build(),
        )

        mockMvc
            .perform(
                delete("/users/me")
                    .with(authentication(userAuth()))
                    .with(csrf()),
            ).andExpect(status().isNoContent)
            .andExpect(header().exists("Set-Cookie"))
    }

    @Test
    fun `인증되지 않은 사용자는 탈퇴할 수 없다`() {
        mockMvc
            .perform(
                delete("/users/me")
                    .with(csrf()),
            ).andExpect(status().isUnauthorized)
    }
}
