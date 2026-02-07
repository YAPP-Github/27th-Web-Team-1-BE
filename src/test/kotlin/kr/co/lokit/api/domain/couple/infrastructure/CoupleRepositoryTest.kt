package kr.co.lokit.api.domain.couple.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.fixture.createUserEntity
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@Import(JpaCoupleRepository::class)
class CoupleRepositoryTest {

    @Autowired
    lateinit var coupleRepository: JpaCoupleRepository

    @Autowired
    lateinit var userJpaRepository: UserJpaRepository

    lateinit var user: UserEntity

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(createUserEntity())
    }

    @Test
    fun `유저와 함께 커플을 저장할 수 있다`() {
        val couple = createCouple(name = "우리 커플")

        val saved = coupleRepository.saveWithUser(couple, user.nonNullId())

        assertNotNull(saved.id)
        assertEquals("우리 커플", saved.name)
        assertNotNull(saved.inviteCode)
        assertEquals(listOf(user.id), saved.userIds)
    }

    @Test
    fun `초대 코드로 커플을 조회할 수 있다`() {
        val saved = coupleRepository.saveWithUser(createCouple(name = "커플"), user.nonNullId())

        val found = coupleRepository.findByInviteCode(saved.inviteCode!!)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `존재하지 않는 초대 코드로 조회하면 null을 반환한다`() {
        val found = coupleRepository.findByInviteCode("nonexist")

        assertNull(found)
    }

    @Test
    fun `커플에 유저를 추가할 수 있다`() {
        val saved = coupleRepository.saveWithUser(createCouple(name = "커플"), user.nonNullId())
        val user2 = userJpaRepository.save(createUserEntity(email = "user2@test.com", name = "유저2"))

        val updated = coupleRepository.addUser(saved.id, user2.nonNullId())

        assertEquals(2, updated.userIds.size)
        assert(updated.userIds.contains(user.id))
        assert(updated.userIds.contains(user2.id))
    }

    @Test
    fun `커플 최대 인원(2명)을 초과하면 예외가 발생한다`() {
        val saved = coupleRepository.saveWithUser(createCouple(name = "커플"), user.nonNullId())
        val user2 = userJpaRepository.save(createUserEntity(email = "user2@test.com", name = "유저2"))
        val user3 = userJpaRepository.save(createUserEntity(email = "user3@test.com", name = "유저3"))

        coupleRepository.addUser(saved.id, user2.nonNullId())

        assertThrows<BusinessException.CoupleMaxMembersExceededException> {
            coupleRepository.addUser(saved.id, user3.nonNullId())
        }
    }
}
