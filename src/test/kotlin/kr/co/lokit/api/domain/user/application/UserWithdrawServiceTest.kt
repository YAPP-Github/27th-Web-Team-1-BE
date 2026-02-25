package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

@ExtendWith(MockitoExtension::class)
class UserWithdrawServiceTest {
    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepositoryPort

    @Mock
    lateinit var cacheManager: CacheManager

    lateinit var userWithdrawService: UserWithdrawService

    @BeforeEach
    fun setUp() {
        userWithdrawService =
            UserWithdrawService(
                userRepository,
                coupleRepository,
                refreshTokenRepository,
                cacheManager,
            )
    }

    @Test
    fun `회원 탈퇴 시 리프레시 토큰을 삭제한다`() {
        val user = createUser(id = 1L, email = "test@test.com")
        whenever(userRepository.findById(1L)).thenReturn(user)

        userWithdrawService.withdraw(1L)

        verify(refreshTokenRepository).deleteByUserId(1L)
    }

    @Test
    fun `회원 탈퇴 시 유저 상태를 WITHDRAWN으로 변경한다`() {
        val user = createUser(id = 1L, email = "test@test.com")
        whenever(userRepository.findById(1L)).thenReturn(user)
        whenever(coupleRepository.findByUserId(1L)).thenReturn(null)

        userWithdrawService.withdraw(1L)

        verify(userRepository).withdraw(1L)
    }

    @Test
    fun `회원 탈퇴 시 캐시를 무효화한다`() {
        val user = createUser(id = 1L, email = "test@test.com")
        whenever(userRepository.findById(1L)).thenReturn(user)
        whenever(coupleRepository.findByUserId(1L)).thenReturn(null)

        val userDetailsCache = mock(Cache::class.java)
        val userCoupleCache = mock(Cache::class.java)
        whenever(cacheManager.getCache("userDetails")).thenReturn(userDetailsCache)
        whenever(cacheManager.getCache("userCouple")).thenReturn(userCoupleCache)

        userWithdrawService.withdraw(1L)

        verify(userDetailsCache).evict("test@test.com")
        verify(userCoupleCache).evict(1L)
    }

    @Test
    fun `존재하지 않는 유저 탈퇴 시 UserNotFoundException이 발생한다`() {
        whenever(userRepository.findById(999L)).thenReturn(null)

        assertThrows<BusinessException.UserNotFoundException> {
            userWithdrawService.withdraw(999L)
        }
    }

    @Test
    fun `연결 끊기가 완료되지 않은 상태에서는 탈퇴할 수 없다`() {
        val user = createUser(id = 1L, email = "test@test.com")
        val couple = createCouple(id = 10L, userIds = listOf(1L, 2L), status = kr.co.lokit.api.common.constants.CoupleStatus.CONNECTED)
        whenever(userRepository.findById(1L)).thenReturn(user)
        whenever(coupleRepository.findByUserId(1L)).thenReturn(couple)

        assertThrows<BusinessException.UserDisconnectRequiredException> {
            userWithdrawService.withdraw(1L)
        }
    }

    @Test
    fun `커플이 DISCONNECTED 상태이면 탈퇴할 수 있다`() {
        val user = createUser(id = 1L, email = "test@test.com")
        val couple = createCouple(id = 10L, userIds = listOf(1L), status = kr.co.lokit.api.common.constants.CoupleStatus.DISCONNECTED)
        whenever(userRepository.findById(1L)).thenReturn(user)
        whenever(coupleRepository.findByUserId(1L)).thenReturn(couple)

        userWithdrawService.withdraw(1L)

        verify(userRepository).withdraw(1L)
    }
}
