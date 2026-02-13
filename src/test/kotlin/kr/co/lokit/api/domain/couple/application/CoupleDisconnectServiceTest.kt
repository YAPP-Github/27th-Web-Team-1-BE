package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.fixture.createCouple
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

@ExtendWith(MockitoExtension::class)
class CoupleDisconnectServiceTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var cacheManager: CacheManager

    @Mock
    lateinit var cache: Cache

    @InjectMocks
    lateinit var coupleDisconnectService: CoupleDisconnectService

    @Test
    fun `커플 연결을 끊을 수 있다`() {
        val couple = createCouple(
            id = 1L,
            name = "우리 커플",
            inviteCode = "12345678",
            userIds = listOf(1L, 2L),
            status = CoupleStatus.CONNECTED,
        )
        `when`(coupleRepository.findByUserId(1L)).thenReturn(couple)
        `when`(cacheManager.getCache("userCouple")).thenReturn(cache)

        coupleDisconnectService.disconnect(1L)

        verify(coupleRepository).disconnect(1L, 1L)
        verify(coupleRepository).removeCoupleUser(1L)
        verify(cache).evict(1L)
        verify(cache).evict(2L)
    }

    @Test
    fun `커플이 없으면 예외가 발생한다`() {
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)

        assertThrows<BusinessException.CoupleNotFoundException> {
            coupleDisconnectService.disconnect(1L)
        }
    }

    @Test
    fun `이미 연결 해제된 커플이면 예외가 발생한다`() {
        val couple = createCouple(
            id = 1L,
            name = "우리 커플",
            inviteCode = "12345678",
            userIds = listOf(1L),
            status = CoupleStatus.DISCONNECTED,
            disconnectedByUserId = 1L,
        )
        `when`(coupleRepository.findByUserId(1L)).thenReturn(couple)

        assertThrows<BusinessException.CoupleAlreadyDisconnectedException> {
            coupleDisconnectService.disconnect(1L)
        }
    }

    @Test
    fun `이미 연결 해제된 상태에서 남아있는 사용자는 추가 연결 끊기가 가능하다`() {
        val couple = createCouple(
            id = 1L,
            name = "우리 커플",
            inviteCode = "12345678",
            userIds = listOf(2L),
            status = CoupleStatus.DISCONNECTED,
            disconnectedByUserId = 1L,
        )
        `when`(coupleRepository.findByUserId(2L)).thenReturn(couple)
        `when`(cacheManager.getCache("userCouple")).thenReturn(cache)

        coupleDisconnectService.disconnect(2L)

        verify(coupleRepository).removeCoupleUser(2L)
        verify(cache).evict(2L)
    }
}
