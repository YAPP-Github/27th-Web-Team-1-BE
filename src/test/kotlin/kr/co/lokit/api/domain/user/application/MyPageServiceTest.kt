package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class MyPageServiceTest {
    @Mock
    lateinit var userRepository: UserRepositoryPort

    @InjectMocks
    lateinit var myPageService: MyPageService

    @Test
    fun `닉네임을 수정할 수 있다`() {
        val user = createUser(id = 1L, name = "기존닉네임")
        val updatedUser = createUser(id = 1L, name = "새닉네임")
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(userRepository.apply(user.copy(name = "새닉네임"))).thenReturn(updatedUser)

        val result = myPageService.updateNickname(1L, "새닉네임")

        assertEquals("새닉네임", result.name)
        verify(userRepository).apply(user.copy(name = "새닉네임"))
    }

    @Test
    fun `존재하지 않는 유저의 닉네임을 수정하면 예외가 발생한다`() {
        `when`(userRepository.findById(1L)).thenReturn(null)

        assertThrows<BusinessException.ResourceNotFoundException> {
            myPageService.updateNickname(1L, "새닉네임")
        }
    }

    @Test
    fun `프로필 사진을 수정할 수 있다`() {
        val user = createUser(id = 1L)
        val updatedUser = createUser(id = 1L).apply { profileImageUrl = "https://example.com/new.jpg" }
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(userRepository.apply(user.copy(profileImageUrl = "https://example.com/new.jpg"))).thenReturn(updatedUser)

        val result = myPageService.updateProfileImage(1L, "https://example.com/new.jpg")

        assertEquals("https://example.com/new.jpg", result.profileImageUrl)
        verify(userRepository).apply(user.copy(profileImageUrl = "https://example.com/new.jpg"))
    }

    @Test
    fun `존재하지 않는 유저의 프로필 사진을 수정하면 예외가 발생한다`() {
        `when`(userRepository.findById(1L)).thenReturn(null)

        assertThrows<BusinessException.ResourceNotFoundException> {
            myPageService.updateProfileImage(1L, "https://example.com/new.jpg")
        }
    }
}
