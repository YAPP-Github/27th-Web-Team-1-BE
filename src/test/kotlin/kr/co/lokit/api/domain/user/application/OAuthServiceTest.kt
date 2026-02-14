package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.user.application.port.OAuthClientPort
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthClientRegistry
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthUserInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension::class)
class OAuthServiceTest {
    @Mock
    lateinit var oAuthClientRegistry: OAuthClientRegistry

    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepositoryPort

    @Mock
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    lateinit var createCoupleUseCase: CreateCoupleUseCase

    @Mock
    lateinit var cacheManager: CacheManager

    lateinit var oAuthService: OAuthService

    @BeforeEach
    fun setUp() {
        oAuthService =
            OAuthService(
                oAuthClientRegistry,
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                createCoupleUseCase,
                lockManager = kr.co.lokit.api.common.concurrency.LockManager(),
                cacheManager,
            )
        whenever(createCoupleUseCase.createIfNone(any(), any())).thenReturn(createCouple(id = 1L, userIds = listOf(1L)))
    }

    private fun setupOAuthClient(
        email: String? = "test@test.com",
        name: String? = "테스트",
        providerId: String = "12345",
    ) {
        val client = mock(OAuthClientPort::class.java)
        val userInfo = mock(OAuthUserInfo::class.java)

        whenever(oAuthClientRegistry.getClient(OAuthProvider.KAKAO)).thenReturn(client)

        whenever(client.getAccessToken("auth-code")).thenReturn("oauth-access-token")
        whenever(client.getUserInfo("oauth-access-token")).thenReturn(userInfo)

        whenever(userInfo.email).thenReturn(email)
        whenever(userInfo.name).thenReturn(name)

        if (email == null) {
            whenever(userInfo.providerId).thenReturn(providerId)
        }
    }

    private fun setupTokenGeneration() {
        whenever(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token")
        whenever(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token")
        whenever(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)
    }

    @Test
    fun `기존 사용자로 로그인하면 토큰을 발급한다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userRepository.update(existingUser.copy(profileImageUrl = null))).thenReturn(existingUser)
        setupTokenGeneration()

        val result = oAuthService.login(OAuthProvider.KAKAO, "auth-code")

        assertNotNull(result)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
    }

    @Test
    fun `이메일이 없으면 KakaoEmailNotProvidedException이 발생한다`() {
        setupOAuthClient(email = null)

        assertThrows<BusinessException.KakaoEmailNotProvidedException> {
            oAuthService.login(OAuthProvider.KAKAO, "auth-code")
        }
    }

    @Test
    fun `토큰 생성 시 기존 리프레시 토큰을 교체한다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userRepository.update(existingUser.copy(profileImageUrl = null))).thenReturn(existingUser)
        setupTokenGeneration()

        oAuthService.login(OAuthProvider.KAKAO, "auth-code")

        verify(refreshTokenRepository).replace(eq(1L), eq("refresh-token"), any())
    }

    @Test
    fun `탈퇴한 사용자가 다시 로그인하면 계정이 복구된다`() {
        setupOAuthClient()
        val withdrawnUser =
            User(
                id = 1L,
                email = "test@test.com",
                name = "테스트",
                status = AccountStatus.WITHDRAWN,
                withdrawnAt = LocalDateTime.now().minusDays(3),
            )

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(withdrawnUser)
        whenever(userRepository.update(withdrawnUser.copy(profileImageUrl = null))).thenReturn(withdrawnUser)
        setupTokenGeneration()

        val userDetailsCache = mock(Cache::class.java)
        val userCoupleCache = mock(Cache::class.java)
        whenever(cacheManager.getCache("userDetails")).thenReturn(userDetailsCache)
        whenever(cacheManager.getCache("userCouple")).thenReturn(userCoupleCache)

        oAuthService.login(OAuthProvider.KAKAO, "auth-code")

        verify(userRepository).reactivate(1L)
        verify(userDetailsCache).evict("test@test.com")
        verify(userCoupleCache).evict(1L)
    }

    @Test
    fun `탈퇴 복구 가능 기간이 만료된 사용자는 로그인 시 예외가 발생한다`() {
        setupOAuthClient()
        val withdrawnExpiredUser =
            User(
                id = 1L,
                email = "test@test.com",
                name = "테스트",
                status = AccountStatus.WITHDRAWN,
                withdrawnAt = LocalDateTime.now().minusDays(32),
            )
        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(withdrawnExpiredUser)
        whenever(userRepository.update(withdrawnExpiredUser.copy(profileImageUrl = null))).thenReturn(withdrawnExpiredUser)

        assertThrows<BusinessException.UserRecoveryExpiredException> {
            oAuthService.login(OAuthProvider.KAKAO, "auth-code")
        }
    }
}
