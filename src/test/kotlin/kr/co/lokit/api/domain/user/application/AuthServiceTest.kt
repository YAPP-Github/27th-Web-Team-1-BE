package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.fixture.createRefreshTokenEntity
import kr.co.lokit.api.fixture.createUserEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock
    lateinit var userJpaRepository: UserJpaRepository

    @Mock
    lateinit var refreshTokenJpaRepository: RefreshTokenJpaRepository

    @Mock
    lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    lateinit var transactionTemplate: TransactionTemplate

    @InjectMocks
    lateinit var authService: AuthService

    private val testUser = User(id = 1L, email = "test@test.com", name = "테스트")

    @Suppress("UNCHECKED_CAST")
    private fun stubTransactionTemplate() {
        doAnswer { invocation ->
            val callback = invocation.getArgument(0) as TransactionCallback<Any>
            callback.doInTransaction(org.mockito.Mockito.mock(TransactionStatus::class.java))
        }.`when`(transactionTemplate).execute(any<TransactionCallback<Any>>())
    }

    private fun stubTokenGeneration(userEntity: kr.co.lokit.api.domain.user.infrastructure.UserEntity) {
        stubTransactionTemplate()
        `when`(userJpaRepository.findById(userEntity.nonNullId())).thenReturn(Optional.of(userEntity))
        `when`(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token")
        `when`(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token")
        `when`(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)
    }

    @Test
    fun `유효한 리프레시 토큰으로 새 토큰 쌍을 발급한다`() {
        val userEntity = createUserEntity(id = 1L, email = "test@test.com")
        val refreshTokenEntity =
            createRefreshTokenEntity(
                token = "valid-token",
                user = userEntity,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenJpaRepository.findByToken("valid-token")).thenReturn(refreshTokenEntity)
        stubTokenGeneration(userEntity)

        val result = authService.refreshIfValid("valid-token")

        assertNotNull(result)
        assertEquals("new-access-token", result.accessToken)
        assertEquals("new-refresh-token", result.refreshToken)
    }

    @Test
    fun `존재하지 않는 리프레시 토큰이면 null을 반환한다`() {
        `when`(refreshTokenJpaRepository.findByToken("unknown-token")).thenReturn(null)

        val result = authService.refreshIfValid("unknown-token")

        assertNull(result)
    }

    @Test
    fun `만료된 리프레시 토큰이면 삭제 후 null을 반환한다`() {
        val userEntity = createUserEntity(id = 1L)
        val expiredToken =
            createRefreshTokenEntity(
                token = "expired-token",
                user = userEntity,
                expiresAt = LocalDateTime.now().minusDays(1),
            )
        `when`(refreshTokenJpaRepository.findByToken("expired-token")).thenReturn(expiredToken)

        val result = authService.refreshIfValid("expired-token")

        assertNull(result)
        verify(refreshTokenJpaRepository).delete(expiredToken)
    }

    @Test
    fun `토큰 재발급 시 기존 리프레시 토큰을 삭제한다`() {
        val userEntity = createUserEntity(id = 1L, email = "test@test.com")
        val refreshTokenEntity =
            createRefreshTokenEntity(
                token = "valid-token",
                user = userEntity,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenJpaRepository.findByToken("valid-token")).thenReturn(refreshTokenEntity)
        stubTokenGeneration(userEntity)

        authService.refreshIfValid("valid-token")

        verify(refreshTokenJpaRepository).deleteByUser(userEntity)
    }

    @Test
    fun `토큰 재발급 시 새 리프레시 토큰이 저장된다`() {
        val userEntity = createUserEntity(id = 1L, email = "test@test.com")
        val refreshTokenEntity =
            createRefreshTokenEntity(
                token = "valid-token",
                user = userEntity,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenJpaRepository.findByToken("valid-token")).thenReturn(refreshTokenEntity)
        stubTokenGeneration(userEntity)

        authService.refreshIfValid("valid-token")

        verify(refreshTokenJpaRepository).save(any(RefreshTokenEntity::class.java))
    }

    @Test
    fun `사용자를 찾을 수 없으면 UserNotFoundException이 발생한다`() {
        val userEntity = createUserEntity(id = 1L, email = "test@test.com")
        val refreshTokenEntity =
            createRefreshTokenEntity(
                token = "valid-token",
                user = userEntity,
                expiresAt = LocalDateTime.now().plusDays(7),
            )
        `when`(refreshTokenJpaRepository.findByToken("valid-token")).thenReturn(refreshTokenEntity)
        stubTransactionTemplate()
        `when`(userJpaRepository.findById(1L)).thenReturn(Optional.empty())
        `when`(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("token")
        `when`(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh")

        assertThrows<ExecutionException> {
            authService.refreshIfValid("valid-token")
        }
    }
}
