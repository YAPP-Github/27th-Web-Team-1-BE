package kr.co.lokit.api.domain.workspace.infrastructure

import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.fixture.createUserEntity
import kr.co.lokit.api.fixture.createWorkspace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@Import(WorkspaceRepositoryImpl::class)
class WorkspaceRepositoryTest {

    @Autowired
    lateinit var workspaceRepository: WorkspaceRepository

    @Autowired
    lateinit var userJpaRepository: UserJpaRepository

    lateinit var user: UserEntity

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(createUserEntity())
    }

    @Test
    fun `유저와 함께 워크스페이스를 저장할 수 있다`() {
        val workspace = createWorkspace(name = "우리 가족")

        val saved = workspaceRepository.saveWithUser(workspace, user.id)

        assertNotNull(saved.id)
        assertEquals("우리 가족", saved.name)
        assertNotNull(saved.inviteCode)
        assertEquals(listOf(user.id), saved.userIds)
    }

    @Test
    fun `초대 코드로 워크스페이스를 조회할 수 있다`() {
        val saved = workspaceRepository.saveWithUser(createWorkspace(name = "팀"), user.id)

        val found = workspaceRepository.findByInviteCode(saved.inviteCode!!)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `존재하지 않는 초대 코드로 조회하면 null을 반환한다`() {
        val found = workspaceRepository.findByInviteCode("nonexist")

        assertNull(found)
    }

    @Test
    fun `워크스페이스에 유저를 추가할 수 있다`() {
        val saved = workspaceRepository.saveWithUser(createWorkspace(name = "팀"), user.id)
        val user2 = userJpaRepository.save(createUserEntity(email = "user2@test.com", name = "유저2"))

        val updated = workspaceRepository.addUser(saved.id, user2.id)

        assertEquals(2, updated.userIds.size)
        assert(updated.userIds.contains(user.id))
        assert(updated.userIds.contains(user2.id))
    }
}
