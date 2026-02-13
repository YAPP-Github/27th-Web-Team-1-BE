package kr.co.lokit.api.domain.couple.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createCoupleRequest
import kr.co.lokit.api.fixture.createJoinCoupleRequest
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CoupleController::class)
class CoupleControllerTest {
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

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
    lateinit var createCoupleUseCase: CreateCoupleUseCase

    @MockitoBean
    lateinit var joinCoupleUseCase: JoinCoupleUseCase

    @MockitoBean
    lateinit var disconnectCoupleUseCase: DisconnectCoupleUseCase

    @MockitoBean
    lateinit var reconnectCoupleUseCase: ReconnectCoupleUseCase

    @Test
    fun `커플 생성 성공`() {
        val savedCouple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678")
        doReturn(savedCouple).`when`(createCoupleUseCase).createIfNone(anyObject(), anyLong())

        mockMvc
            .perform(
                post("/couples")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createCoupleRequest(name = "우리 커플"))),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `커플 생성 실패 - 이름이 비어있음`() {
        mockMvc
            .perform(
                post("/couples")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createCoupleRequest(name = ""))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `초대 코드로 커플 합류 성공`() {
        val couple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678", userIds = listOf(1L, 2L))
        doReturn(couple).`when`(joinCoupleUseCase).joinByInviteCode(anyString(), anyLong())

        mockMvc
            .perform(
                post("/couples/join")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createJoinCoupleRequest())),
            ).andExpect(status().isOk)
    }

    @Test
    fun `초대 코드로 커플 합류 실패 - 잘못된 초대 코드`() {
        doThrow(BusinessException.ResourceNotFoundException("유효하지 않은 초대 코드입니다"))
            .`when`(joinCoupleUseCase)
            .joinByInviteCode(anyString(), anyLong())

        mockMvc
            .perform(
                post("/couples/join")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createJoinCoupleRequest(inviteCode = "invalid1"))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `초대 코드로 커플 합류 실패 - 초대 코드가 8자가 아님`() {
        mockMvc
            .perform(
                post("/couples/join")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createJoinCoupleRequest(inviteCode = "1234"))),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `재연결 성공`() {
        val couple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678", userIds = listOf(1L, 2L))
        doReturn(couple).`when`(reconnectCoupleUseCase).reconnect(anyLong())

        mockMvc
            .perform(
                post("/couples/reconnect")
                    .with(authentication(userAuth()))
                    .with(csrf()),
            ).andExpect(status().isOk)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc
            .perform(
                post("/couples")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createCoupleRequest())),
            ).andExpect(status().isUnauthorized)
    }
}
