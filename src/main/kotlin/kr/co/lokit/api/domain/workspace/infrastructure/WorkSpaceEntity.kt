package kr.co.lokit.api.domain.workspace.infrastructure

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.common.util.InviteCodeGenerator
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity

@Entity
class WorkSpaceEntity(
    @Column(nullable = false, length = 20)
    val name: String,
) : BaseEntity() {
    @Column(unique = true, length = 8)
    var inviteCode: String = InviteCodeGenerator.generate()

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "workspace",
    )
    var albums: MutableList<AlbumEntity> = mutableListOf()
        protected set

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        mappedBy = "workspace",
    )
    var workspaceUsers: MutableList<WorkspaceUserEntity> = mutableListOf()
        protected set

    fun addUser(workspaceUser: WorkspaceUserEntity) {
        workspaceUsers.add(workspaceUser)
    }
}
