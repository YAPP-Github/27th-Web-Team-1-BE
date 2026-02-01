package kr.co.lokit.api.domain.workspace.application

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
) {
    @Transactional
    fun createIfNone(workspace: Workspace, userId: Long): Workspace =
        workspaceRepository.findByUserId(userId) ?: workspaceRepository.saveWithUser(workspace, userId)

    @Transactional
    fun joinByInviteCode(inviteCode: String, userId: Long): Workspace {
        val workspace = workspaceRepository.findByInviteCode(inviteCode)
            ?: throw entityNotFound<Workspace>("inviteCode", inviteCode)
        return workspaceRepository.addUser(workspace.id, userId)
    }
}
