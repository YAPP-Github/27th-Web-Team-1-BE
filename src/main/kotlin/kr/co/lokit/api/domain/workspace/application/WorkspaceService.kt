package kr.co.lokit.api.domain.workspace.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
) {
    @Transactional
    fun create(workspace: Workspace, userId: Long): Workspace =
        workspaceRepository.saveWithUser(workspace, userId)

    @Transactional
    fun joinByInviteCode(inviteCode: String, userId: Long): Workspace {
        val workspace = workspaceRepository.findByInviteCode(inviteCode)
            ?: throw BusinessException.ResourceNotFoundException("유효하지 않은 초대 코드입니다")
        return workspaceRepository.addUser(workspace.id, userId)
    }
}
