package kr.co.lokit.api.domain.workspace.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkspaceTest {

    @Test
    fun `정상적으로 워크스페이스를 생성할 수 있다`() {
        val workspace = Workspace(name = "우리 가족")

        assertEquals("우리 가족", workspace.name)
        assertEquals(0L, workspace.id)
    }

    @Test
    fun `기본값이 올바르게 설정된다`() {
        val workspace = Workspace(name = "테스트")

        assertEquals(0L, workspace.id)
        assertNull(workspace.inviteCode)
        assertEquals(emptyList(), workspace.userIds)
    }

    @Test
    fun `모든 필드를 지정하여 워크스페이스를 생성할 수 있다`() {
        val workspace = Workspace(
            id = 1L,
            name = "팀 워크스페이스",
            inviteCode = "12345678",
            userIds = listOf(1L, 2L, 3L),
        )

        assertEquals(1L, workspace.id)
        assertEquals("팀 워크스페이스", workspace.name)
        assertEquals("12345678", workspace.inviteCode)
        assertEquals(listOf(1L, 2L, 3L), workspace.userIds)
    }
}
