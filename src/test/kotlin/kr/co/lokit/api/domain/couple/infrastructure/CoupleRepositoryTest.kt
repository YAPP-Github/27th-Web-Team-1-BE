package kr.co.lokit.api.domain.couple.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.fixture.createUserEntity
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.common.constants.CoupleStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        assertEquals(listOf(user.nonNullId()), saved.userIds)
    }

    @Test
    fun `커플에 유저를 추가할 수 있다`() {
        val saved = coupleRepository.saveWithUser(createCouple(name = "커플"), user.nonNullId())
        val user2 = userJpaRepository.save(createUserEntity(email = "user2@test.com", name = "유저2"))

        val updated = coupleRepository.addUser(saved.id, user2.nonNullId())

        assertEquals(2, updated.userIds.size)
        assert(updated.userIds.contains(user.nonNullId()))
        assert(updated.userIds.contains(user2.nonNullId()))
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

    @Test
    fun `동일 유저가 여러 커플에 존재해도 CONNECTED 커플을 우선 반환한다`() {
        val connected = coupleRepository.saveWithUser(createCouple(name = "active"), user.nonNullId())
        val partner = userJpaRepository.save(createUserEntity(email = "partner@test.com", name = "파트너"))
        coupleRepository.addUser(connected.id, partner.nonNullId())

        coupleRepository.saveWithUser(
            createCouple(name = "old", status = CoupleStatus.DISCONNECTED),
            user.nonNullId(),
        )

        val found = coupleRepository.findByUserId(user.nonNullId())

        assertNotNull(found)
        assertEquals(connected.id, found.id)
        assertEquals(CoupleStatus.CONNECTED, found.status)
    }
}
