package kr.co.lokit.api.domain.workspace.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.mapping.toDomain
import kr.co.lokit.api.domain.workspace.mapping.toEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class WorkspaceRepositoryImpl(
    private val workspaceJpaRepository: WorkspaceJpaRepository,
    private val userJpaRepository: UserJpaRepository,
) : WorkspaceRepository {
    override fun save(workspace: Workspace): Workspace {
        val entity = workspace.toEntity()
        return workspaceJpaRepository.save(entity).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): Workspace? =
        workspaceJpaRepository.findByIdFetchUsers(id)?.toDomain()

    @Transactional
    override fun saveWithUser(workspace: Workspace, userId: Long): Workspace {
        val userEntity = userJpaRepository.findByIdOrNull(userId)
            ?: throw entityNotFound<User>(userId)

        val workspaceEntity = workspace.toEntity()
        val savedWorkspace = workspaceJpaRepository.save(workspaceEntity)

        val workspaceUser = WorkspaceUserEntity(
            workspace = savedWorkspace,
            user = userEntity,
        )
        savedWorkspace.addUser(workspaceUser)

        return savedWorkspace.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByInviteCode(inviteCode: String): Workspace? =
        workspaceJpaRepository.findByInviteCode(inviteCode)?.toDomain()

    @Transactional
    override fun addUser(workspaceId: Long, userId: Long): Workspace {
        val workspaceEntity = workspaceJpaRepository.findByIdFetchUsers(workspaceId)
            ?: throw entityNotFound<Workspace>(workspaceId)
        val userEntity = userJpaRepository.findByIdOrNull(userId)
            ?: throw entityNotFound<User>(userId)

        val workspaceUser = WorkspaceUserEntity(
            workspace = workspaceEntity,
            user = userEntity,
        )
        workspaceEntity.addUser(workspaceUser)

        return workspaceEntity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByUserId(userId: Long): Workspace? =
        workspaceJpaRepository.findByUserId(userId)?.toDomain()
}
