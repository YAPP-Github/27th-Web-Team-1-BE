package kr.co.lokit.api.domain.user.infrastructure

import kr.co.lokit.api.fixture.createUserEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class JpaUserRepositoryTest {
    @Mock
    lateinit var userJpaRepository: UserJpaRepository

    lateinit var repository: JpaUserRepository

    @BeforeEach
    fun setUp() {
        repository = JpaUserRepository(userJpaRepository)
    }

    @Test
    fun `이메일로 활성 유저를 찾으면 그대로 반환한다`() {
        val entity = createUserEntity(id = 1L, email = "active@test.com", name = "active")
        whenever(userJpaRepository.findByEmail("active@test.com")).thenReturn(entity)

        val found = repository.findByEmail("active@test.com")

        assertEquals(1L, found.id)
        assertEquals("active@test.com", found.email)
    }

    @Test
    fun `활성 유저가 없고 soft delete 유저가 있으면 복구 후 반환한다`() {
        val restored = createUserEntity(id = 2L, email = "restore@test.com", name = "restored")
        whenever(userJpaRepository.findByEmail("restore@test.com")).thenReturn(null, restored)
        whenever(userJpaRepository.restoreDeletedByEmail("restore@test.com")).thenReturn(1)

        val found = repository.findByEmail("restore@test.com")

        assertEquals(2L, found.id)
        verify(userJpaRepository).restoreDeletedByEmail("restore@test.com")
    }

    @Test
    fun `복구 대상이 없으면 신규 유저를 생성한다`() {
        val created = createUserEntity(id = 3L, email = "new@test.com", name = "new")
        whenever(userJpaRepository.findByEmail("new@test.com")).thenReturn(null)
        whenever(userJpaRepository.restoreDeletedByEmail("new@test.com")).thenReturn(0)
        whenever(userJpaRepository.save(any<UserEntity>())).thenReturn(created)

        val found = repository.findByEmail("new@test.com")

        assertEquals(3L, found.id)
        assertEquals("new@test.com", found.email)
        verify(userJpaRepository).save(any<UserEntity>())
    }

    @Test
    fun `복구 쿼리 성공 후 재조회가 실패하면 예외가 발생한다`() {
        whenever(userJpaRepository.findByEmail("broken@test.com")).thenReturn(null)
        whenever(userJpaRepository.restoreDeletedByEmail("broken@test.com")).thenReturn(1)

        assertThrows(IllegalStateException::class.java) {
            repository.findByEmail("broken@test.com")
        }
    }
}
