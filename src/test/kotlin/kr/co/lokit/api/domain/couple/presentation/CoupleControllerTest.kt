package kr.co.lokit.api.domain.couple.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.common.constants.CoupleCookieStatus
import kr.co.lokit.api.domain.couple.application.CoupleCommandService
import kr.co.lokit.api.domain.couple.application.CoupleCookieStatusResolver
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.PartnerSummaryReadModel
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.fixture.createCoupleRequest
import kr.co.lokit.api.fixture.createJoinCoupleRequest
import kr.co.lokit.api.fixture.createUpdateFirstMetDateRequest
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CoupleController::class)
class CoupleControllerTest {
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Autowired
    lateinit var mockMvc: MockMvc

    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

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
    lateinit var coupleCookieStatusResolver: CoupleCookieStatusResolver

    @MockitoBean
    lateinit var createCoupleUseCase: CreateCoupleUseCase

    @MockitoBean
    lateinit var disconnectCoupleUseCase: DisconnectCoupleUseCase

    @MockitoBean
    lateinit var reconnectCoupleUseCase: ReconnectCoupleUseCase

    @MockitoBean
    lateinit var coupleInviteUseCase: CoupleInviteUseCase

    @MockitoBean
    lateinit var coupleCommandService: CoupleCommandService

    @MockitoBean
    lateinit var cacheManager: CacheManager

    @Test
    fun `커플 생성 엔드포인트 없음`() {
        mockMvc
            .perform(
                post("/couples")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createCoupleRequest(name = "우리 커플"))),
            ).andExpect(status().isNotFound)
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
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `초대 코드로 커플 합류 성공`() {
        val linked =
            CoupleStatusReadModel(
                isCoupled = true,
                partnerSummary = PartnerSummaryReadModel(userId = 2L, nickname = "테스트", profileImageUrl = null),
            )
        doReturn(CoupleCookieStatus.COUPLED).`when`(coupleCookieStatusResolver).resolve(anyLong())
        doReturn(ResponseCookie.from("coupleStatus", "COUPLED").build())
            .`when`(cookieGenerator)
            .createCoupleStatusCookie(anyObject(), anyObject())
        doReturn(linked).`when`(coupleInviteUseCase).joinByInviteCode(anyLong(), anyString(), anyString())

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
        doThrow(BusinessException.InviteCodeNotFoundException())
            .`when`(coupleInviteUseCase)
            .joinByInviteCode(anyLong(), anyString(), anyString())

        mockMvc
            .perform(
                post("/couples/join")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createJoinCoupleRequest(inviteCode = "123456"))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `초대 코드로 커플 합류 실패 - 초대 코드가 6자가 아님`() {
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
        val coupled =
            CoupleStatusReadModel(
                isCoupled = true,
                partnerSummary = PartnerSummaryReadModel(userId = 2L, nickname = "테스트", profileImageUrl = null),
            )
        doReturn(CoupleCookieStatus.COUPLED).`when`(coupleCookieStatusResolver).resolve(anyLong())
        doReturn(ResponseCookie.from("coupleStatus", "COUPLED").build())
            .`when`(cookieGenerator)
            .createCoupleStatusCookie(anyObject(), anyObject())
        doReturn(coupled).`when`(reconnectCoupleUseCase).reconnect(anyLong())

        mockMvc
            .perform(
                post("/couples/reconnect")
                    .with(authentication(userAuth()))
                    .with(csrf()),
            ).andExpect(status().isOk)
    }

    @Test
    fun `처음 만난 날짜 수정 성공`() {
        doNothing().`when`(coupleCommandService).updateFirstMetDate(anyLong(), anyObject())

        mockMvc
            .perform(
                patch("/couples/me/first-met-date")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createUpdateFirstMetDateRequest())),
            ).andExpect(status().isNoContent)
    }

    @Test
    fun `처음 만난 날짜 수정 실패 - 커플 없음`() {
        doThrow(BusinessException.CoupleNotFoundException())
            .`when`(coupleCommandService)
            .updateFirstMetDate(anyLong(), anyObject())

        mockMvc
            .perform(
                patch("/couples/me/first-met-date")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createUpdateFirstMetDateRequest())),
            ).andExpect(status().isNotFound)
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
