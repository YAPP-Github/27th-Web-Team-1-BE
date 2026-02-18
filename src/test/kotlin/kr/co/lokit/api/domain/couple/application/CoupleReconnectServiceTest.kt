package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
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
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CoupleReconnectServiceTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var cacheManager: CacheManager

    @Mock
    lateinit var cache: Cache

    @InjectMocks
    lateinit var coupleReconnectService: CoupleReconnectService

    @Test
    fun `재연결에 성공한다`() {
        val disconnectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(2L),
            status = CoupleStatus.DISCONNECTED,
            disconnectedAt = LocalDateTime.now().minusDays(10),
            disconnectedByUserId = 1L,
        )
        val reconnectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(1L, 2L),
            status = CoupleStatus.CONNECTED,
        )
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(disconnectedCouple)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)
        `when`(coupleRepository.reconnect(1L, 1L)).thenReturn(reconnectedCouple)
        `when`(userRepository.findById(2L)).thenReturn(createUser(id = 2L, name = "파트너"))
        `when`(cacheManager.getCache("userCouple")).thenReturn(cache)

        val result = coupleReconnectService.reconnect(1L)

        assertEquals(true, result.isCoupled)
        assertEquals(2L, result.partnerSummary?.userId)
        verify(coupleRepository).reconnect(1L, 1L)
        verify(cache).evict(1L)
        verify(cache).evict(2L)
    }

    @Test
    fun `기존 솔로 커플이 있으면 삭제 후 재연결한다`() {
        val disconnectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(2L),
            status = CoupleStatus.DISCONNECTED,
            disconnectedAt = LocalDateTime.now().minusDays(5),
            disconnectedByUserId = 1L,
        )
        val existingSoloCouple = createCouple(
            id = 3L,
            name = "솔로",
            userIds = listOf(1L),
        )
        val reconnectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(1L, 2L),
            status = CoupleStatus.CONNECTED,
        )
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(disconnectedCouple)
        `when`(coupleRepository.findByUserId(1L)).thenReturn(existingSoloCouple)
        `when`(coupleRepository.findById(3L)).thenReturn(existingSoloCouple)
        `when`(coupleRepository.reconnect(1L, 1L)).thenReturn(reconnectedCouple)
        `when`(userRepository.findById(2L)).thenReturn(createUser(id = 2L, name = "파트너"))
        `when`(cacheManager.getCache("userCouple")).thenReturn(cache)

        val result = coupleReconnectService.reconnect(1L)

        assertEquals(true, result.isCoupled)
        verify(coupleRepository).deleteById(3L)
        verify(coupleRepository).reconnect(1L, 1L)
    }

    @Test
    fun `DISCONNECTED 상태가 아니면 예외가 발생한다`() {
        val connectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(1L, 2L),
            status = CoupleStatus.CONNECTED,
        )
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(connectedCouple)

        assertThrows<BusinessException.CoupleNotDisconnectedException> {
            coupleReconnectService.reconnect(1L)
        }
    }

    @Test
    fun `유예기간이 만료되면 예외가 발생한다`() {
        val expiredCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = listOf(2L),
            status = CoupleStatus.DISCONNECTED,
            disconnectedAt = LocalDateTime.now().minusDays(32),
            disconnectedByUserId = 1L,
        )
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(expiredCouple)

        assertThrows<BusinessException.CoupleReconnectExpiredException> {
            coupleReconnectService.reconnect(1L)
        }
    }

    @Test
    fun `DISCONNECTED 커플이 없으면 예외가 발생한다`() {
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(null)

        assertThrows<BusinessException.ResourceNotFoundException> {
            coupleReconnectService.reconnect(1L)
        }
    }

    @Test
    fun `잔존 사용자가 없으면 재연결할 수 없다`() {
        val disconnectedCouple = createCouple(
            id = 1L,
            name = "우리 커플",
            userIds = emptyList(),
            status = CoupleStatus.DISCONNECTED,
            disconnectedAt = LocalDateTime.now().minusDays(2),
            disconnectedByUserId = 1L,
        )
        `when`(coupleRepository.findByDisconnectedByUserId(1L)).thenReturn(disconnectedCouple)

        assertThrows<BusinessException.CoupleReconnectNotAllowedException> {
            coupleReconnectService.reconnect(1L)
        }
    }
}
