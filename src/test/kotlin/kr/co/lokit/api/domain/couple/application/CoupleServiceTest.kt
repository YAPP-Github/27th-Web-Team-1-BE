package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createUser
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CoupleServiceTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var coupleProfileImageUrlResolver: CoupleProfileImageUrlResolver

    @InjectMocks
    lateinit var coupleCommandService: CoupleCommandService

    @Test
    fun `커플이 없으면 생성한다`() {
        val input = createCouple(name = "우리 커플")
        val saved = createCouple(id = 1L, name = "우리 커플", userIds = listOf(1L))
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)
        `when`(coupleRepository.saveWithUser(input, 1L)).thenReturn(saved)
        `when`(coupleProfileImageUrlResolver.resolve(any())).thenReturn("https://cdn.example.com/default/lock.png")
        `when`(userRepository.findById(1L)).thenReturn(createUser(id = 1L))

        val result = coupleCommandService.createIfNone(input, 1L)

        assertEquals(1L, result.id)
        assertEquals("우리 커플", result.name)
        assertEquals(listOf(1L), result.userIds)
    }

    @Test
    fun `이미 커플이 있으면 기존 커플을 반환한다`() {
        val existing = createCouple(id = 10L, name = "기존", userIds = listOf(1L))
        `when`(coupleRepository.findByUserId(1L)).thenReturn(existing)

        val result = coupleCommandService.createIfNone(createCouple(name = "새 커플"), 1L)

        assertEquals(10L, result.id)
        verify(coupleRepository).findByUserId(1L)
    }
}
