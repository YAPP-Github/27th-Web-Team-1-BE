package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class MyPageServiceTest {
    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var photoRepository: PhotoRepositoryPort

    @InjectMocks
    lateinit var myPageService: MyPageService

    @Test
    fun `닉네임을 수정할 수 있다`() {
        val user = createUser(id = 1L, name = "기존닉네임")
        val updatedUser = createUser(id = 1L, name = "새닉네임")
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(userRepository.update(user.copy(name = "새닉네임"))).thenReturn(updatedUser)

        val result = myPageService.updateNickname(1L, "새닉네임")

        assertEquals("새닉네임", result.name)
        verify(userRepository).update(user.copy(name = "새닉네임"))
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
        `when`(
            userRepository.update(user.copy(profileImageUrl = "https://example.com/new.jpg")),
        ).thenReturn(updatedUser)

        val result = myPageService.updateProfileImage(1L, "https://example.com/new.jpg")

        assertEquals("https://example.com/new.jpg", result.profileImageUrl)
        verify(userRepository).update(user.copy(profileImageUrl = "https://example.com/new.jpg"))
    }

    @Test
    fun `존재하지 않는 유저의 프로필 사진을 수정하면 예외가 발생한다`() {
        `when`(userRepository.findById(1L)).thenReturn(null)

        assertThrows<BusinessException.ResourceNotFoundException> {
            myPageService.updateProfileImage(1L, "https://example.com/new.jpg")
        }
    }

    @Test
    fun `firstMetDate가 설정되어 있으면 coupledDay가 계산된다`() {
        val me = createUser(id = 1L, name = "나")
        val partner = createUser(id = 2L, name = "파트너")
        val couple =
            createCouple(
                id = 1L,
                userIds = listOf(1L, 2L),
                firstMetDate = LocalDate.now().minusDays(9),
            )
        `when`(userRepository.findById(1L)).thenReturn(me)
        `when`(userRepository.findById(2L)).thenReturn(partner)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(couple)
        `when`(photoRepository.countByCoupleId(1L)).thenReturn(5L)

        val result = myPageService.getMyPage(1L)

        assertEquals("test@test.com", result.myEmail)
        assertEquals(LocalDate.now().minusDays(9), result.firstMetDate)
        assertEquals(10L, result.coupledDay)
    }

    @Test
    fun `firstMetDate가 설정되지 않으면 coupledDay가 null이다`() {
        val me = createUser(id = 1L, name = "나")
        val partner = createUser(id = 2L, name = "파트너")
        val couple =
            createCouple(
                id = 1L,
                userIds = listOf(1L, 2L),
                firstMetDate = null,
            )
        `when`(userRepository.findById(1L)).thenReturn(me)
        `when`(userRepository.findById(2L)).thenReturn(partner)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(couple)
        `when`(photoRepository.countByCoupleId(1L)).thenReturn(0L)

        val result = myPageService.getMyPage(1L)

        assertNull(result.firstMetDate)
        assertNull(result.coupledDay)
    }

    @Test
    fun `커플이 아니면 coupledDay가 null이다`() {
        val me = createUser(id = 1L, name = "나")
        `when`(userRepository.findById(1L)).thenReturn(me)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)

        val result = myPageService.getMyPage(1L)

        assertNull(result.firstMetDate)
        assertNull(result.coupledDay)
        assertNull(result.partnerName)
    }

    @Test
    fun `마이페이지 조회 시 내 이메일이 포함된다`() {
        val me = createUser(id = 1L, name = "나", email = "myemail@test.com")
        `when`(userRepository.findById(1L)).thenReturn(me)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)

        val result = myPageService.getMyPage(1L)

        assertEquals("myemail@test.com", result.myEmail)
    }
}
