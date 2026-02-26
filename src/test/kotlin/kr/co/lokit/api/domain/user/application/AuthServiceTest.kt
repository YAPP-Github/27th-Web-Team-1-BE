package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRecord
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.domain.user.domain.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock
    lateinit var userRepository: UserRepositoryPort

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepositoryPort

    @Mock
    lateinit var jwtTokenProvider: JwtTokenProvider

    @InjectMocks
    lateinit var authService: AuthService

    private val testUser = User(id = 1L, email = "test@test.com", name = "테스트")

    @Test
    fun `유효한 리프레시 토큰으로 새 토큰 쌍을 발급한다`() {
        val refreshTokenRecord =
            RefreshTokenRecord(
                userId = 1L,
                expiresAt = LocalDateTime.now().plusDays(7),
            )

        `when`(refreshTokenRepository.findByToken("valid-token")).thenReturn(refreshTokenRecord)
        `when`(userRepository.findById(1L)).thenReturn(testUser)
        `when`(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token")
        `when`(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token")
        `when`(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)

        val result = authService.refreshIfValid("valid-token")

        assertNotNull(result)
        assertEquals("new-access-token", result.accessToken)
        assertEquals("new-refresh-token", result.refreshToken)
    }

    @Test
    fun `존재하지 않는 리프레시 토큰이면 null을 반환한다`() {
        `when`(refreshTokenRepository.findByToken("unknown-token")).thenReturn(null)

        val result = authService.refreshIfValid("unknown-token")

        assertNull(result)
    }

    @Test
    fun `만료된 리프레시 토큰이면 삭제 후 null을 반환한다`() {
        val expiredRecord =
            RefreshTokenRecord(
                userId = 1L,
                expiresAt = LocalDateTime.now().minusDays(1),
            )
        `when`(refreshTokenRepository.findByToken("expired-token")).thenReturn(expiredRecord)

        val result = authService.refreshIfValid("expired-token")

        assertNull(result)
        verify(refreshTokenRepository).deleteByToken("expired-token")
    }

    @Test
    fun `토큰 재발급 시 새 리프레시 토큰이 저장된다`() {
        val refreshTokenRecord =
            RefreshTokenRecord(
                userId = 1L,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenRepository.findByToken("valid-token")).thenReturn(refreshTokenRecord)
        `when`(userRepository.findById(1L)).thenReturn(testUser)
        `when`(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token")
        `when`(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token")
        `when`(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)

        authService.refreshIfValid("valid-token")

        verify(refreshTokenRepository).replace(
            org.mockito.kotlin.eq(1L),
            org.mockito.kotlin.eq("new-refresh-token"),
            org.mockito.kotlin.any(),
        )
    }

    @Test
    fun `로그아웃 시 리프레시 토큰이 삭제된다`() {
        authService.logout(1L)

        verify(refreshTokenRepository).deleteByUserId(1L)
    }

    @Test
    fun `사용자를 찾을 수 없으면 UserNotFoundException이 발생한다`() {
        val refreshTokenRecord =
            RefreshTokenRecord(
                userId = 1L,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenRepository.findByToken("valid-token")).thenReturn(refreshTokenRecord)
        `when`(userRepository.findById(1L)).thenReturn(null)

        assertThrows<BusinessException.UserNotFoundException> {
            authService.refreshIfValid("valid-token")
        }
    }
}
