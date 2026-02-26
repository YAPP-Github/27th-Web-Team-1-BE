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
    fun `현재 커플이 CONNECTED 단독이고 disconnect 이력이 없으면 NOT_COUPLED를 반환한다`() {
        val userId = 1L
        val currentCouple =
            createCouple(
                id = 100L,
                name = "default",
                userIds = listOf(userId),
                status = CoupleStatus.CONNECTED,
            )

        `when`(coupleRepository.findByUserIdFresh(userId)).thenReturn(currentCouple)
        `when`(coupleRepository.findByDisconnectedByUserId(userId)).thenReturn(null)

        val result = coupleCookieStatusResolver.resolve(userId)

        assertEquals(CoupleCookieStatus.NOT_COUPLED, result)
        verify(coupleRepository).findByDisconnectedByUserId(userId)
    }

    @Test
    fun `currentCouple가 없어도 31일 내 disconnect 이력이 있으면 DISCONNECTED_BY_ME를 반환한다`() {
        val userId = 1L
        val disconnectedByMe =
            createCouple(
                id = 200L,
                name = "old",
                userIds = emptyList(),
                status = CoupleStatus.DISCONNECTED,
                disconnectedAt = LocalDateTime.now().minusDays(2),
                disconnectedByUserId = userId,
            )

        `when`(coupleRepository.findByUserIdFresh(userId)).thenReturn(null)
        `when`(coupleRepository.findByDisconnectedByUserId(userId)).thenReturn(disconnectedByMe)

        val result = coupleCookieStatusResolver.resolve(userId)

        assertEquals(CoupleCookieStatus.DISCONNECTED_BY_ME, result)
    }

    @Test
    fun `현재 커플이 CONNECTED 단독이어도 reconnect 가능 대상이 있으면 DISCONNECTED_BY_ME를 반환한다`() {
        val userId = 1L
        val partnerId = 2L
        val currentSoloCouple =
            createCouple(
                id = 210L,
                name = "default",
                userIds = listOf(userId),
                status = CoupleStatus.CONNECTED,
            )
        val disconnectedByMe =
            createCouple(
                id = 200L,
                name = "old",
                userIds = listOf(partnerId),
                status = CoupleStatus.DISCONNECTED,
                disconnectedAt = LocalDateTime.now().minusDays(5),
                disconnectedByUserId = userId,
            )

        `when`(coupleRepository.findByUserIdFresh(userId)).thenReturn(currentSoloCouple)
        `when`(coupleRepository.findByDisconnectedByUserId(userId)).thenReturn(disconnectedByMe)

        val result = coupleCookieStatusResolver.resolve(userId)

        assertEquals(CoupleCookieStatus.DISCONNECTED_BY_ME, result)
    }

    @Test
    fun `상대가 나가고 나는 남아있는 경우 DISCONNECTED_BY_PARTNER를 반환한다`() {
        val me = 2L
        val disconnectedByPartner =
            createCouple(
                id = 300L,
                name = "old-couple",
                userIds = listOf(me),
                status = CoupleStatus.DISCONNECTED,
                disconnectedAt = LocalDateTime.now().minusDays(10),
                disconnectedByUserId = 1L,
            )

        `when`(coupleRepository.findByUserIdFresh(me)).thenReturn(disconnectedByPartner)

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

        `when`(coupleRepository.findByUserIdFresh(me)).thenReturn(expiredDisconnected)

        val result = coupleCookieStatusResolver.resolve(me)

        assertEquals(CoupleCookieStatus.DISCONNECTED_EXPIRED, result)
        verify(coupleRepository, never()).findByUserIdFresh(partner)
        verify(coupleRepository, never()).findByDisconnectedByUserId(me)
    }
}
