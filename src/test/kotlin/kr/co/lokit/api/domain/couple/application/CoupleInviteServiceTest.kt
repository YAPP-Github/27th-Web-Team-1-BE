package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.InviteCodeRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.InviteCode
import kr.co.lokit.api.domain.couple.domain.InviteCodeStatus
import kr.co.lokit.api.domain.couple.domain.InviteIssuer
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.cache.CacheManager
import java.time.LocalDateTime
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

    @Mock
    lateinit var coupleProfileImageUrlResolver: CoupleProfileImageUrlResolver

    @Mock
    lateinit var createCoupleUseCase: CreateCoupleUseCase

    lateinit var coupleInviteService: CoupleInviteService

    @BeforeEach
    fun setUp() {
        lenient().`when`(coupleProfileImageUrlResolver.resolve(any())).thenReturn("https://cdn.example.com/default/lock.png")
        coupleInviteService =
            CoupleInviteService(
                coupleRepository = coupleRepository,
                userRepository = userRepository,
                inviteCodeRepository = inviteCodeRepository,
                cacheManager = cacheManager,
                rateLimiter = CoupleInviteRateLimiter(),
                coupleProfileImageUrlResolver = coupleProfileImageUrlResolver,
                createCoupleUseCase = createCoupleUseCase,
            )
    }

    @Test
    fun `이미 커플인 사용자의 join은 현재 상태를 그대로 반환한다`() {
        val userId = 10L
        val partnerId = 20L
        val coupled = createCouple(id = 1L, userIds = listOf(userId, partnerId))
        `when`(coupleRepository.findByUserId(userId)).thenReturn(coupled)
        `when`(userRepository.findById(partnerId)).thenReturn(createUser(id = partnerId, name = "partner"))

        val result = coupleInviteService.joinByInviteCode(userId, "ABC123", "127.0.0.1")

        assertTrue(result.isCoupled)
        assertEquals(partnerId, result.partnerSummary?.userId)
        verify(inviteCodeRepository, never()).findByCodeForUpdate("ABC123")
    }

    @Test
    fun `join 시 초대코드 입력자의 기존 미완성 커플을 제거하고 합류시킨다`() {
        val userId = 10L
        val inviterId = 20L
        val inviteCode = "123456"
        val joinerExistingCouple = createCouple(id = 100L, name = "default", userIds = listOf(userId))
        val inviterCouple = createCouple(id = 200L, name = "default", userIds = listOf(inviterId))
        val joinedCouple = createCouple(id = 200L, name = "default", userIds = listOf(inviterId, userId))
        val invite =
            InviteCode(
                id = 1L,
                code = inviteCode,
                createdBy = InviteIssuer(userId = inviterId, name = "inviter", profileImageUrl = null),
                status = InviteCodeStatus.UNUSED,
                expiresAt = LocalDateTime.now().plusHours(1),
            )

        `when`(coupleRepository.findByUserId(userId))
            .thenReturn(joinerExistingCouple, joinerExistingCouple, joinerExistingCouple)
        `when`(coupleRepository.findByUserId(inviterId)).thenReturn(inviterCouple, inviterCouple)
        `when`(coupleRepository.findById(joinerExistingCouple.id)).thenReturn(joinerExistingCouple)
        `when`(inviteCodeRepository.findByCodeForUpdate(inviteCode)).thenReturn(invite)
        `when`(coupleRepository.addUser(inviterCouple.id, userId)).thenReturn(joinedCouple)
        `when`(userRepository.findById(inviterId)).thenReturn(createUser(id = inviterId, name = "inviter"))
        `when`(userRepository.findById(userId)).thenReturn(createUser(id = userId, name = "joiner"))

        val result = coupleInviteService.joinByInviteCode(userId, inviteCode, "127.0.0.1")

        assertTrue(result.isCoupled)
        assertEquals(inviterId, result.partnerSummary?.userId)
        val ordered = inOrder(coupleRepository)
        ordered.verify(coupleRepository).deleteById(joinerExistingCouple.id)
        ordered.verify(coupleRepository).addUser(inviterCouple.id, userId)
    }
}
