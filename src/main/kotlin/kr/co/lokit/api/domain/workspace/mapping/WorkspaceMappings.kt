package kr.co.lokit.api.domain.workspace.mapping

import kr.co.lokit.api.domain.workspace.domain.WorkSpace
import kr.co.lokit.api.domain.workspace.infrastructure.WorkSpaceEntity

fun WorkSpace.toEntity(): WorkSpaceEntity =
    WorkSpaceEntity(
        name = this.name,
    )

fun WorkSpaceEntity.toDomain(): WorkSpace =
    WorkSpace(
        id = this.id,
        name = this.name,
        inviteCode = this.inviteCode,
        userIds = this.workspaceUsers.map { it.user.id },
    )
