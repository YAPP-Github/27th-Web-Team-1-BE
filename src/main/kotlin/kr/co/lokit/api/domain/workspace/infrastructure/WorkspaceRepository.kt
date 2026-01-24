package kr.co.lokit.api.domain.workspace.infrastructure

import kr.co.lokit.api.domain.workspace.domain.WorkSpace

interface WorkspaceRepository {
    fun save(workspace: WorkSpace): WorkSpace

    fun findById(id: Long): WorkSpace?

    fun saveWithUser(workspace: WorkSpace, userId: Long): WorkSpace

    fun findByInviteCode(inviteCode: String): WorkSpace?

    fun addUser(workspaceId: Long, userId: Long): WorkSpace
}
