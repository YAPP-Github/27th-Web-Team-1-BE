package kr.co.lokit.api.domain.photo.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.domain.photo.application.PhotoService
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(PhotoController::class)
class PhotoControllerTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Autowired
    lateinit var mockMvc: MockMvc

    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    @MockitoBean
    lateinit var compositeAuthenticationResolver: CompositeAuthenticationResolver

    @MockitoBean
    lateinit var photoService: PhotoService

    private fun userAuth(): UsernamePasswordAuthenticationToken {
        val userEntity = UserEntity(email = "test@test.com", name = "테스트", role = UserRole.USER)
        val field = userEntity.javaClass.superclass.getDeclaredField("_id")
        field.isAccessible = true
        field.set(userEntity, 1L)
        return UsernamePasswordAuthenticationToken(userEntity, null, userEntity.authorities)
    }

    @Test
    fun `사진 생성 성공`() {
        val savedPhoto = Photo(id = 1L, albumId = 1L, location = Location(127.0, 37.5))
        doReturn(savedPhoto).`when`(photoService).create(anyObject())

        mockMvc.perform(
            post("/photos")
                .with(authentication(userAuth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        CreatePhotoRequest(
                            url = "https://example.com/photo.jpg",
                            albumId = 1L,
                            longitude = 127.0,
                            latitude = 37.5,
                            takenAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                        ),
                    ),
                ),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `사진 상세 조회 성공`() {
        val response = PhotoDetailResponse(
            id = 1L,
            url = "https://example.com/photo.jpg",
            takenAt = "2026.01.01",
            albumName = "여행",
            uploaderName = "테스트",
            address = "서울 강남구",
            description = "테스트",
        )
        doReturn(response).`when`(photoService).getPhotoDetail(1L)

        mockMvc.perform(
            get("/photos/1")
                .with(user("test").roles("USER")),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `존재하지 않는 사진 상세 조회 실패`() {
        doThrow(BusinessException.ResourceNotFoundException("Photo(id=999)을(를) 찾을 수 없습니다"))
            .`when`(photoService).getPhotoDetail(999L)

        mockMvc.perform(
            get("/photos/999")
                .with(user("test").roles("USER")),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `사진 수정 성공`() {
        val updatedPhoto = Photo(id = 1L, albumId = 1L, location = Location(127.0, 37.5), description = "수정됨")
        doReturn(updatedPhoto).`when`(photoService).update(anyLong(), anyObject())

        mockMvc.perform(
            put("/photos/1")
                .with(user("test").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdatePhotoRequest(description = "수정됨"))),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `사진 삭제 성공`() {
        doNothing().`when`(photoService).delete(1L)

        mockMvc.perform(
            delete("/photos/1")
                .with(user("test").roles("USER"))
                .with(csrf()),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc.perform(get("/photos/1"))
            .andExpect(status().isUnauthorized)
    }
}
