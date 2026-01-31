package kr.co.lokit.api.domain.workspace.mapping

import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity

fun Workspace.toEntity(): WorkspaceEntity =
    WorkspaceEntity(
        name = this.name,
    )

fun WorkspaceEntity.toDomain(): Workspace =
    Workspace(
        id = this.nonNullId(),
        name = this.name,
        inviteCode = this.inviteCode,
        userIds = this.workspaceUsers.map { it.user.nonNullId() },
    )
