package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constants.CoupleCookieStatus
import kr.co.lokit.api.common.constants.CoupleStatus
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CoupleCookieStatusResolverTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @InjectMocks
    lateinit var coupleCookieStatusResolver: CoupleCookieStatusResolver

    @Test
    fun `현재 커플이 CONNECTED 단독 상태면 NOT_COUPLED를 반환한다`() {
        val userId = 1L
        val currentCouple =
            createCouple(
                id = 100L,
                name = "default",
                userIds = listOf(userId),
                status = CoupleStatus.CONNECTED,
            )

        `when`(coupleRepository.findByUserId(userId)).thenReturn(currentCouple)

        val result = coupleCookieStatusResolver.resolve(userId)

        assertEquals(CoupleCookieStatus.NOT_COUPLED, result)
        verify(coupleRepository, never()).findByDisconnectedByUserId(userId)
    }

    @Test
    fun `currentCouple가 없을 때만 disconnectedByMe fallback을 사용한다`() {
        val userId = 1L
        val disconnectedByMe =
            createCouple(
                id = 200L,
                name = "old",
                status = CoupleStatus.DISCONNECTED,
                disconnectedByUserId = userId,
            )

        `when`(coupleRepository.findByUserId(userId)).thenReturn(null)
        `when`(coupleRepository.findByDisconnectedByUserId(userId)).thenReturn(disconnectedByMe)

        val result = coupleCookieStatusResolver.resolve(userId)

        assertEquals(CoupleCookieStatus.DISCONNECTED_BY_ME, result)
    }

    @Test
    fun `상대가 나가고 나는 남아있는 경우 DISCONNECTED_BY_PARTNER를 반환한다`() {
        val me = 2L
        val partner = 1L
        val disconnectedByPartner =
            createCouple(
                id = 300L,
                name = "old-couple",
                userIds = listOf(me),
                status = CoupleStatus.DISCONNECTED,
                disconnectedAt = LocalDateTime.now().minusDays(10),
                disconnectedByUserId = partner,
            )

        `when`(coupleRepository.findByUserId(me)).thenReturn(disconnectedByPartner)
        `when`(coupleRepository.findByUserId(partner)).thenReturn(null)

        val result = coupleCookieStatusResolver.resolve(me)

        assertEquals(CoupleCookieStatus.DISCONNECTED_BY_PARTNER, result)
        verify(coupleRepository, never()).findByDisconnectedByUserId(me)
    }

    @Test
    fun `상대가 나가고 31일 초과면 DISCONNECTED_EXPIRED를 반환한다`() {
        val me = 2L
        val partner = 1L
        val expiredDisconnected =
            createCouple(
                id = 301L,
                name = "old-couple",
                userIds = listOf(me),
                status = CoupleStatus.DISCONNECTED,
                disconnectedAt = LocalDateTime.now().minusDays(32),
                disconnectedByUserId = partner,
            )

        `when`(coupleRepository.findByUserId(me)).thenReturn(expiredDisconnected)

        val result = coupleCookieStatusResolver.resolve(me)

        assertEquals(CoupleCookieStatus.DISCONNECTED_EXPIRED, result)
        verify(coupleRepository, never()).findByUserId(partner)
        verify(coupleRepository, never()).findByDisconnectedByUserId(me)
    }
}
