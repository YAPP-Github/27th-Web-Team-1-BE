package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
import kr.co.lokit.api.domain.workspace.domain.Workspace
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TempLoginService(
    private val userRepository: UserRepository,
    private val workspaceService: WorkspaceService
) {
    @Transactional
    fun login(email: String): Long = (userRepository.findByEmail(email) ?: saveUserOrFindOnConflict(email)).id

    private fun saveUserOrFindOnConflict(email: String): User =
        try {
            val user = userRepository.save(User(email = email, name = "user"))
            workspaceService.createIfNone(Workspace(name = "default"), user.id)
            user
        } catch (e: DataIntegrityViolationException) {
            userRepository.findByEmail(email)
                ?: throw e
        }
}
