package kr.co.lokit.api.domain.workspace.infrastructure

import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.hibernate.annotations.Immutable

@Immutable
@Entity(name = "WorkspaceUser")
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_id", "user_id"])],
    indexes = [Index(columnList = "workspace_id"), Index(columnList = "user_id")],
)
class WorkspaceUserEntity(
    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    val workspace: WorkspaceEntity,
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
) : BaseEntity()
