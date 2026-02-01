package kr.co.lokit.api.domain.workspace.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceRepository
import kr.co.lokit.api.fixture.createWorkspace
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class WorkspaceServiceTest {

    @Mock
    lateinit var workspaceRepository: WorkspaceRepository

    @InjectMocks
    lateinit var workspaceService: WorkspaceService

    @Test
    fun `워크스페이스를 생성할 수 있다`() {
        val workspace = createWorkspace(name = "우리 가족")
        val savedWorkspace = createWorkspace(id = 1L, name = "우리 가족", inviteCode = "12345678", userIds = listOf(1L))
        `when`(workspaceRepository.saveWithUser(workspace, 1L)).thenReturn(savedWorkspace)

        val result = workspaceService.createIfNone(workspace, 1L)

        assertEquals(1L, result.id)
        assertEquals("우리 가족", result.name)
        assertEquals("12345678", result.inviteCode)
        assertEquals(listOf(1L), result.userIds)
    }

    @Test
    fun `유효한 초대 코드로 워크스페이스에 합류할 수 있다`() {
        val workspace = createWorkspace(id = 1L, name = "팀", inviteCode = "12345678")
        val joinedWorkspace = createWorkspace(id = 1L, name = "팀", inviteCode = "12345678", userIds = listOf(1L, 2L))
        `when`(workspaceRepository.findByInviteCode("12345678")).thenReturn(workspace)
        `when`(workspaceRepository.addUser(1L, 2L)).thenReturn(joinedWorkspace)

        val result = workspaceService.joinByInviteCode("12345678", 2L)

        assertEquals(1L, result.id)
        assertEquals(listOf(1L, 2L), result.userIds)
    }

    @Test
    fun `잘못된 초대 코드로 합류하면 예외가 발생한다`() {
        `when`(workspaceRepository.findByInviteCode("invalid1")).thenReturn(null)

        val exception = assertThrows<BusinessException.ResourceNotFoundException> {
            workspaceService.joinByInviteCode("invalid1", 1L)
        }

        assertEquals("Workspace을(를) (invalid1)로 찾을 수 없습니다", exception.message)
    }
}
