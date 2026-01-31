package kr.co.lokit.api.domain.workspace.infrastructure

import kr.co.lokit.api.domain.workspace.domain.Workspace

interface WorkspaceRepository {
    fun save(workspace: Workspace): Workspace

    fun findById(id: Long): Workspace?

    fun saveWithUser(workspace: Workspace, userId: Long): Workspace

    fun findByInviteCode(inviteCode: String): Workspace?

    fun addUser(workspaceId: Long, userId: Long): Workspace

    fun findByUserId(userId: Long): Workspace?
}
