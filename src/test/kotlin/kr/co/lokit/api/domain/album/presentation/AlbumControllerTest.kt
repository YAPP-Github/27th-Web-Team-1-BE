package kr.co.lokit.api.domain.album.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.permission.PermissionService
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.album.application.port.`in`.CreateAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.GetAlbumUseCase
import kr.co.lokit.api.domain.album.application.port.`in`.UpdateAlbumUseCase
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.UpdateAlbumTitleRequest
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createAlbumRequest
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AlbumController::class)
class AlbumControllerTest {

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
    lateinit var createAlbumUseCase: CreateAlbumUseCase

    @MockitoBean
    lateinit var getAlbumUseCase: GetAlbumUseCase

    @MockitoBean
    lateinit var updateAlbumUseCase: UpdateAlbumUseCase

    @MockitoBean
    lateinit var permissionService: PermissionService

    @Test
    fun `앨범 생성 성공`() {
        val savedAlbum = createAlbum(id = 1L, title = "여행")
        doReturn(savedAlbum).`when`(createAlbumUseCase).create(anyObject(), anyLong())

        mockMvc.perform(
            post("/albums")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAlbumRequest(title = "여행"))),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `앨범 생성 실패 - 제목이 10자 초과`() {
        mockMvc.perform(
            post("/albums")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AlbumRequest(title = "12345678901"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `앨범 제목 수정 성공`() {
        val updatedAlbum = createAlbum(id = 1L, title = "새 제목")
        doReturn(updatedAlbum).`when`(updateAlbumUseCase).updateTitle(anyLong(), anyString())

        mockMvc.perform(
            patch("/albums/1")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateAlbumTitleRequest(title = "새 제목"))),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `앨범 제목 수정 실패 - 제목이 10자 초과`() {
        mockMvc.perform(
            patch("/albums/1")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateAlbumTitleRequest(title = "12345678901"))),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `앨범 삭제 성공`() {
        doNothing().`when`(updateAlbumUseCase).delete(anyLong())

        mockMvc.perform(
            delete("/albums/1")
                .with(authentication(userAuth()))
                .with(csrf()),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `앨범 삭제 실패 - 존재하지 않는 앨범`() {
        doThrow(BusinessException.ResourceNotFoundException("Album(id=999)을(를) 찾을 수 없습니다"))
            .`when`(updateAlbumUseCase).delete(anyLong())

        mockMvc.perform(
            delete("/albums/999")
                .with(authentication(userAuth()))
                .with(csrf()),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc.perform(
            get("/albums/selectable"),
        )
            .andExpect(status().isUnauthorized)
    }
}
