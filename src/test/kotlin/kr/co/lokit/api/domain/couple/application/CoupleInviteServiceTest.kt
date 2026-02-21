package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.InviteCodeRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class CoupleInviteServiceTest {
    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var inviteCodeRepository: InviteCodeRepositoryPort

    @Mock
    lateinit var cacheManager: CacheManager

    lateinit var coupleInviteService: CoupleInviteService

    @BeforeEach
    fun setUp() {
        coupleInviteService =
            CoupleInviteService(
                coupleRepository = coupleRepository,
                userRepository = userRepository,
                inviteCodeRepository = inviteCodeRepository,
                cacheManager = cacheManager,
                rateLimiter = CoupleInviteRateLimiter(),
            )
    }

    @Test
    fun `이미 커플인 사용자의 confirm은 현재 상태를 그대로 반환한다`() {
        val userId = 10L
        val partnerId = 20L
        val coupled = createCouple(id = 1L, userIds = listOf(userId, partnerId))
        `when`(coupleRepository.findByUserId(userId)).thenReturn(coupled)
        `when`(userRepository.findById(partnerId)).thenReturn(createUser(id = partnerId, name = "partner"))

        val result = coupleInviteService.confirmInviteCode(userId, "ABC123", "127.0.0.1")

        assertTrue(result.isCoupled)
        assertEquals(partnerId, result.partnerSummary?.userId)
        verify(inviteCodeRepository, never()).findByCodeForUpdate("ABC123")
    }
}
