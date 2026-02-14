package kr.co.lokit.api.domain.user.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.port.`in`.UpdateMyPageUseCase
import kr.co.lokit.api.domain.user.dto.UpdateNicknameRequest
import kr.co.lokit.api.domain.user.dto.UpdateProfileImageRequest
import kr.co.lokit.api.fixture.createUpdateNicknameRequest
import kr.co.lokit.api.fixture.createUpdateProfileImageRequest
import kr.co.lokit.api.fixture.createUser
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MyPageController::class)
class MyPageControllerTest {
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
    lateinit var updateMyPageUseCase: UpdateMyPageUseCase

    @Test
    fun `닉네임 수정 성공`() {
        val user = createUser(id = 1L, name = "새닉네임")
        doReturn(user).`when`(updateMyPageUseCase).updateNickname(anyLong(), anyString())

        mockMvc.perform(
            patch("/my-page/nickname")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUpdateNicknameRequest())),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `닉네임 수정 실패 - 빈 값`() {
        mockMvc.perform(
            patch("/my-page/nickname")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateNicknameRequest(nickname = ""))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `닉네임 수정 실패 - 10자 초과`() {
        mockMvc.perform(
            patch("/my-page/nickname")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateNicknameRequest(nickname = "12345678901"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `프로필 사진 수정 성공`() {
        val user = createUser(id = 1L)
        doReturn(user).`when`(updateMyPageUseCase).updateProfileImage(anyLong(), anyString())

        mockMvc.perform(
            put("/my-page/profile-image")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUpdateProfileImageRequest())),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `프로필 사진 수정 실패 - URL 빈 값`() {
        mockMvc.perform(
            put("/my-page/profile-image")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateProfileImageRequest(profileImageUrl = ""))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc.perform(
            patch("/my-page/nickname")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUpdateNicknameRequest())),
        )
            .andExpect(status().isUnauthorized)
    }
}
