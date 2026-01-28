package kr.co.lokit.api.domain.workspace.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface WorkspaceJpaRepository : JpaRepository<WorkspaceEntity, Long> {
    fun findByInviteCode(inviteCode: String): WorkspaceEntity?
}
