package kr.co.lokit.api.domain.user.application

import jakarta.persistence.EntityManager
import kr.co.lokit.api.common.concurrency.LockManager
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.user.application.port.OAuthClientPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthClientRegistry
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthUserInfo
import kr.co.lokit.api.fixture.createUserEntity
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
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
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
    lateinit var userJpaRepository: UserJpaRepository

    @Mock
    lateinit var refreshTokenJpaRepository: RefreshTokenJpaRepository

    @Mock
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    lateinit var createCoupleUseCase: CreateCoupleUseCase

    @Mock
    lateinit var entityManager: EntityManager

    lateinit var oAuthService: OAuthService

    @BeforeEach
    fun setUp() {
        oAuthService =
            OAuthService(
                oAuthClientRegistry,
                userRepository,
                userJpaRepository,
                refreshTokenJpaRepository,
                jwtTokenProvider,
                createCoupleUseCase,
                lockManager = LockManager(),
            )
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

    private fun setupTokenGeneration(user: User) {
        whenever(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token")
        whenever(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token")
        whenever(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)
    }

    @Test
    fun `기존 사용자로 로그인하면 토큰을 발급한다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")
        val existingUserEntity = createUserEntity(id = 1L, email = existingUser.email)

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userJpaRepository.findByEmail("test@test.com")).thenReturn(existingUserEntity)
        setupTokenGeneration(existingUser)

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
    fun `토큰 생성 시 사용자를 찾을 수 없으면 UserNotFoundException이 발생한다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userJpaRepository.findByEmail("test@test.com")).thenReturn(null)
        setupTokenGeneration(existingUser)

        assertThrows<BusinessException.UserNotFoundException> {
            oAuthService.login(OAuthProvider.KAKAO, "auth-code")
        }
    }

    @Test
    fun `토큰 생성 시 기존 리프레시 토큰을 삭제한다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")
        val existingUserEntity = createUserEntity(id = 1L, email = existingUser.email)

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userJpaRepository.findByEmail("test@test.com")).thenReturn(existingUserEntity)
        setupTokenGeneration(existingUser)

        oAuthService.login(OAuthProvider.KAKAO, "auth-code")

        verify(refreshTokenJpaRepository).deleteByUser(existingUserEntity)
    }

    @Test
    fun `토큰 생성 시 새 리프레시 토큰이 저장된다`() {
        setupOAuthClient()
        val existingUser = User(id = 1L, email = "test@test.com", name = "테스트")
        val existingUserEntity = createUserEntity(id = 1L, email = existingUser.email)

        whenever(userRepository.findByEmail("test@test.com", "테스트")).thenReturn(existingUser)
        whenever(userJpaRepository.findByEmail("test@test.com")).thenReturn(existingUserEntity)
        setupTokenGeneration(existingUser)

        oAuthService.login(OAuthProvider.KAKAO, "auth-code")

        verify(refreshTokenJpaRepository).save(any<RefreshTokenEntity>())
    }
}
