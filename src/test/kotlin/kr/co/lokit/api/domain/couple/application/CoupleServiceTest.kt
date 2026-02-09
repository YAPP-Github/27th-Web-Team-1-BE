package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CoupleServiceTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @InjectMocks
    lateinit var coupleCommandService: CoupleCommandService

    @Test
    fun `커플을 생성할 수 있다`() {
        val couple = createCouple(name = "우리 커플")
        val savedCouple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678", userIds = listOf(1L))
        `when`(coupleRepository.saveWithUser(couple, 1L)).thenReturn(savedCouple)

        val result = coupleCommandService.createIfNone(couple, 1L)

        assertEquals(1L, result.id)
        assertEquals("우리 커플", result.name)
        assertEquals("12345678", result.inviteCode)
        assertEquals(listOf(1L), result.userIds)
    }

    @Test
    fun `유효한 초대 코드로 커플에 합류할 수 있다`() {
        val couple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678")
        val joinedCouple = createCouple(id = 1L, name = "우리 커플", inviteCode = "12345678", userIds = listOf(1L, 2L))
        `when`(coupleRepository.findByInviteCode("12345678")).thenReturn(couple)
        `when`(coupleRepository.addUser(1L, 2L)).thenReturn(joinedCouple)

        val result = coupleCommandService.joinByInviteCode("12345678", 2L)

        assertEquals(1L, result.id)
        assertEquals(listOf(1L, 2L), result.userIds)
    }

    @Test
    fun `잘못된 초대 코드로 합류하면 예외가 발생한다`() {
        `when`(coupleRepository.findByInviteCode("invalid1")).thenReturn(null)

        val exception =
            assertThrows<BusinessException.ResourceNotFoundException> {
                coupleCommandService.joinByInviteCode("invalid1", 1L)
            }

        assertEquals("Couple을(를) (invalid1)로 찾을 수 없습니다", exception.message)
    }
}
