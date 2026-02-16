package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.constant.CoupleStatus
import kr.co.lokit.api.common.annotation.RateLimit
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.domain.InviteCodeStatus
import kr.co.lokit.api.domain.couple.dto.CoupleLinkResponse
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.PartnerSummaryResponse
import kr.co.lokit.api.domain.couple.infrastructure.InviteCodeEntity
import kr.co.lokit.api.domain.couple.infrastructure.InviteCodeJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class CoupleInviteService(
    private val coupleRepository: CoupleRepositoryPort,
    private val userJpaRepository: UserJpaRepository,
    private val inviteCodeJpaRepository: InviteCodeJpaRepository,
    private val cacheManager: CacheManager,
    private val rateLimiter: CoupleInviteRateLimiter,
) : CoupleInviteUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun getMyStatus(userId: Long): CoupleStatusResponse {
        val couple = coupleRepository.findByUserId(userId) ?: return CoupleStatusResponse(isCoupled = false)
        if (couple.status != CoupleStatus.CONNECTED || !couple.isFull()) {
            return CoupleStatusResponse(isCoupled = false)
        }

        val partnerId = couple.userIds.firstOrNull { it != userId } ?: return CoupleStatusResponse(isCoupled = false)
        return CoupleStatusResponse(
            isCoupled = true,
            partnerSummary = partnerSummary(partnerId),
        )
    }

    @Transactional
    @RateLimit(
        key = "'invite:create:' + #userId",
        windowSeconds = 60,
        maxRequests = 5,
    )
    override fun generateInviteCode(
        userId: Long,
    ): InviteCodeResponse {
        validateIssuerReady(userId)

        val now = LocalDateTime.now()
        val active = inviteCodeJpaRepository.findActiveUnusedByUserIdForUpdate(userId, now).firstOrNull()
        if (active != null) {
            return InviteCodeResponse.from(active.code, active.expiresAt)
        }

        val created = issueNewInviteCode(userId, now)
        log.info(
            "invite_created inviterUserId={} inviteId={} expiresAt={}",
            userId,
            created.nonNullId(),
            created.expiresAt,
        )
        return InviteCodeResponse.from(created.code, created.expiresAt)
    }

    @Transactional
    @RateLimit(
        key = "'invite:create:' + #userId",
        windowSeconds = 60,
        maxRequests = 5,
    )
    override fun refreshInviteCode(
        userId: Long,
    ): InviteCodeResponse {
        validateIssuerReady(userId)

        val now = LocalDateTime.now()
        inviteCodeJpaRepository.findActiveUnusedByUserIdForUpdate(userId, now).forEach {
            inviteCodeJpaRepository.hardDeleteById(it.nonNullId())
        }
        val created = issueNewInviteCode(userId, now)
        log.info(
            "invite_created inviterUserId={} inviteId={} expiresAt={}",
            userId,
            created.nonNullId(),
            created.expiresAt,
        )
        return InviteCodeResponse.from(created.code, created.expiresAt)
    }

    @Transactional
    override fun revokeInviteCode(
        userId: Long,
        inviteCode: String,
    ) {
        val entity =
            inviteCodeJpaRepository.findByCodeForUpdate(inviteCode)
                ?: throw BusinessException.InviteCodeNotFoundException()

        if (entity.createdBy.nonNullId() != userId) {
            throw BusinessException.InviteNotOwnerException()
        }
        if (entity.status == InviteCodeStatus.USED) {
            throw BusinessException.InviteAlreadyUsedException()
        }
        if (entity.status == InviteCodeStatus.UNUSED) {
            inviteCodeJpaRepository.hardDeleteById(entity.nonNullId())
            log.info("invite_revoked_deleted inviterUserId={} inviteId={}", userId, entity.nonNullId())
        }
    }

    @Transactional(readOnly = true)
    override fun verifyInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): InviteCodePreviewResponse {
        rateLimiter.checkVerificationAllowed(userId, clientIp)
        if (!INVITE_CODE_FORMAT.matches(inviteCode)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteInvalidFormatException()
        }
        validateNotCoupled(userId)

        val entity =
            inviteCodeJpaRepository.findByCode(inviteCode)
                ?: run {
                    rateLimiter.recordVerificationFailure(userId, clientIp)
                    throw BusinessException.InviteCodeNotFoundException()
                }

        if (entity.createdBy.nonNullId() == userId) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.SelfInviteNotAllowedException()
        }
        validateInviteState(entity, userId, clientIp)

        rateLimiter.clearVerificationFailures(userId, clientIp)
        log.info("invite_verified verifierUserId={} inviteId={}", userId, entity.nonNullId())
        return InviteCodePreviewResponse(
            inviterUserId = entity.createdBy.nonNullId(),
            nickname = entity.createdBy.name,
            profileImageUrl = entity.createdBy.profileImageUrl,
        )
    }

    @Transactional
    override fun confirmInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): CoupleLinkResponse {
        rateLimiter.checkVerificationAllowed(userId, clientIp)
        if (!INVITE_CODE_FORMAT.matches(inviteCode)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteInvalidFormatException()
        }

        val invite =
            inviteCodeJpaRepository.findByCodeForUpdate(inviteCode)
                ?: run {
                    rateLimiter.recordVerificationFailure(userId, clientIp)
                    throw BusinessException.InviteCodeNotFoundException()
                }
        val inviterId = invite.createdBy.nonNullId()
        if (inviterId == userId) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.SelfInviteNotAllowedException()
        }

        val lockIds = listOf(userId, inviterId).distinct().sorted()
        userJpaRepository.findAllByIdInForUpdate(lockIds)

        if (isCoupled(userId) || isCoupled(inviterId)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteAlreadyCoupledException()
        }

        validateInviteState(invite, userId, clientIp)

        val inviterCouple =
            coupleRepository.findByUserId(inviterId)
                ?: throw BusinessException.InviteCodeNotFoundException()

        try {
            val joined = coupleRepository.addUser(inviterCouple.id, userId)
            inviteCodeJpaRepository.hardDeleteById(invite.nonNullId())

            cacheManager.evictUserCoupleCache(userId, inviterId)
            cacheManager.clearPermissionCaches()
            rateLimiter.clearVerificationFailures(userId, clientIp)

            val partnerId = joined.userIds.first { it != userId }
            log.info("couple_linked inviterUserId={} joinerUserId={} coupleId={}", inviterId, userId, joined.id)
            return CoupleLinkResponse(
                coupleId = joined.id,
                partnerSummary = partnerSummary(partnerId),
            )
        } catch (_: DataIntegrityViolationException) {
            throw BusinessException.InviteRaceConflictException(
                errors = errorDetailsOf(ErrorField.USER_ID to userId),
            )
        }
    }

    private fun validateNotCoupled(userId: Long) {
        if (isCoupled(userId)) {
            throw BusinessException.InviteAlreadyCoupledException()
        }
    }

    private fun validateIssuerReady(userId: Long) {
        val couple = coupleRepository.findByUserId(userId) ?: throw BusinessException.CoupleNotFoundException()
        if (couple.status != CoupleStatus.CONNECTED) {
            throw BusinessException.CoupleNotFoundException()
        }
        if (couple.isFull()) {
            throw BusinessException.InviteAlreadyCoupledException()
        }
    }

    private fun isCoupled(userId: Long): Boolean {
        val couple = coupleRepository.findByUserId(userId) ?: return false
        return couple.status == CoupleStatus.CONNECTED && couple.isFull()
    }

    private fun validateInviteState(
        invite: InviteCodeEntity,
        userId: Long,
        clientIp: String,
    ) {
        if (invite.isExpired()) {
            inviteCodeJpaRepository.hardDeleteById(invite.nonNullId())
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteCodeExpiredException()
        }

        when (invite.status) {
            InviteCodeStatus.UNUSED -> {
                Unit
            }

            InviteCodeStatus.USED -> {
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeUsedException()
            }

            InviteCodeStatus.REVOKED -> {
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeRevokedException()
            }

            InviteCodeStatus.EXPIRED -> {
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeExpiredException()
            }
        }
    }

    private fun issueNewInviteCode(
        userId: Long,
        now: LocalDateTime,
    ): InviteCodeEntity {
        val inviter =
            userJpaRepository.findByIdOrNull(userId)
                ?: throw BusinessException.UserNotFoundException()

        repeat(CODE_RETRY_LIMIT) {
            val newCode = secureRandom.nextInt(CODE_BOUND).toString().padStart(INVITE_CODE_LENGTH, '0')
            if (!inviteCodeJpaRepository.existsByCode(newCode)) {
                return inviteCodeJpaRepository.save(
                    InviteCodeEntity(
                        code = newCode,
                        createdBy = inviter,
                        status = InviteCodeStatus.UNUSED,
                        expiresAt = now.plusHours(INVITE_EXPIRATION_HOURS),
                    ),
                )
            }
        }
        throw BusinessException.InviteRaceConflictException()
    }

    private fun partnerSummary(partnerId: Long): PartnerSummaryResponse {
        val partner =
            userJpaRepository.findByIdOrNull(partnerId)
                ?: throw BusinessException.UserNotFoundException()
        return PartnerSummaryResponse(
            userId = partner.nonNullId(),
            nickname = partner.name,
            profileImageUrl = partner.profileImageUrl,
        )
    }

    companion object {
        private const val INVITE_EXPIRATION_HOURS = 24L
        private const val CODE_RETRY_LIMIT = 5
        private const val INVITE_CODE_LENGTH = 6
        private const val CODE_BOUND = 1_000_000
        private val INVITE_CODE_FORMAT = Regex("^\\d{6}$")
        private val secureRandom = SecureRandom()
    }
}
