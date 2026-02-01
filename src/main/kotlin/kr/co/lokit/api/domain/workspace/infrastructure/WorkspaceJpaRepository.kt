package kr.co.lokit.api.domain.workspace.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface WorkspaceJpaRepository : JpaRepository<WorkspaceEntity, Long> {
    @Query(
        """
        select w from Workspace w
        left join fetch w.workspaceUsers wu
        left join fetch wu.user
        where w.inviteCode = :inviteCode
        """
    )
    fun findByInviteCode(inviteCode: String): WorkspaceEntity?

    @Query(
        """
        select w from Workspace w
        left join fetch w.workspaceUsers wu
        left join fetch wu.user
        where w.id = :id
        """
    )
    fun findByIdFetchUsers(id: Long): WorkspaceEntity?

    @Query(
        """
        select w from Workspace w
        join w.workspaceUsers wu
        where wu.user.id = :userId
        """
    )
    fun findByUserId(userId: Long): WorkspaceEntity?
}
