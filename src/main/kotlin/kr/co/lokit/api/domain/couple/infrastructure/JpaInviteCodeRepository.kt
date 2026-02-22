package kr.co.lokit.api.domain.couple.infrastructure

import kr.co.lokit.api.common.exception.entityNotFound
import kr.co.lokit.api.domain.couple.application.port.InviteCodeRepositoryPort
import kr.co.lokit.api.domain.couple.domain.InviteCode
import kr.co.lokit.api.domain.couple.infrastructure.mapping.toDomain
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class JpaInviteCodeRepository(
    private val inviteCodeJpaRepository: InviteCodeJpaRepository,
    private val userJpaRepository: kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository,
) : InviteCodeRepositoryPort {
    @Transactional(readOnly = true)
    override fun existsByCode(code: String): Boolean = inviteCodeJpaRepository.existsByCode(code)

    @Transactional(readOnly = true)
    override fun findByCode(code: String): InviteCode? = inviteCodeJpaRepository.findByCode(code)?.toDomain()

    @Transactional(readOnly = true)
    override fun findByCodeForUpdate(code: String): InviteCode? =
        inviteCodeJpaRepository.findByCodeForUpdate(code)?.toDomain()

    @Transactional(readOnly = true)
    override fun findActiveUnusedByUserIdForUpdate(
        userId: Long,
        now: LocalDateTime,
    ): List<InviteCode> = inviteCodeJpaRepository.findActiveUnusedByUserIdForUpdate(userId, now).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findExpiredUnusedByUserIdForUpdate(
        userId: Long,
        now: LocalDateTime,
    ): List<InviteCode> = inviteCodeJpaRepository.findExpiredUnusedByUserIdForUpdate(userId, now).map { it.toDomain() }

    @Transactional
    override fun createUnused(
        code: String,
        createdByUserId: Long,
        expiresAt: LocalDateTime,
    ): InviteCode {
        val creator =
            userJpaRepository.findByIdOrNull(createdByUserId)
                ?: throw entityNotFound<kr.co.lokit.api.domain.user.infrastructure.UserEntity>(createdByUserId)
        return inviteCodeJpaRepository
            .save(InviteCodeEntity(code = code, createdBy = creator, expiresAt = expiresAt))
            .toDomain()
    }

    @Transactional
    override fun deleteById(id: Long) {
        inviteCodeJpaRepository.deleteById(id)
    }
}
