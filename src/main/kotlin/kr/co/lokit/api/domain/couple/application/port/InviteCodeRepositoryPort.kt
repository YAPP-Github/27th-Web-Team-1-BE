package kr.co.lokit.api.domain.couple.application.port

import kr.co.lokit.api.domain.couple.domain.InviteCode
import java.time.LocalDateTime

interface InviteCodeRepositoryPort {
    fun existsByCode(code: String): Boolean

    fun findByCode(code: String): InviteCode?

    fun findByCodeForUpdate(code: String): InviteCode?

    fun findActiveUnusedByUserIdForUpdate(
        userId: Long,
        now: LocalDateTime,
    ): List<InviteCode>

    fun findExpiredUnusedByUserIdForUpdate(
        userId: Long,
        now: LocalDateTime,
    ): List<InviteCode>

    fun createUnused(
        code: String,
        createdByUserId: Long,
        expiresAt: LocalDateTime,
    ): InviteCode

    fun deleteById(id: Long)
}
