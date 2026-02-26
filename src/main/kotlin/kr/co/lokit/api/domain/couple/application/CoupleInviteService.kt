package kr.co.lokit.api.domain.couple.application

import kr.co.lokit.api.common.annotation.RateLimit
import kr.co.lokit.api.common.constants.CoupleStatus
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import kr.co.lokit.api.config.cache.clearPermissionCaches
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.domain.couple.application.mapping.toCoupledStatusReadModel
import kr.co.lokit.api.domain.couple.application.mapping.toIssueReadModel
import kr.co.lokit.api.domain.couple.application.mapping.toPreviewReadModel
import kr.co.lokit.api.domain.couple.application.mapping.toUncoupledStatusReadModel
import kr.co.lokit.api.domain.couple.application.mapping.uncoupledStatusReadModel
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.InviteCodeRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.domain.CoupleProfileImage
import kr.co.lokit.api.domain.couple.domain.CoupleStatusReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCode
import kr.co.lokit.api.domain.couple.domain.InviteCodeIssueReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodePolicy
import kr.co.lokit.api.domain.couple.domain.InviteCodePreviewReadModel
import kr.co.lokit.api.domain.couple.domain.InviteCodeRejectionReason
import kr.co.lokit.api.domain.couple.domain.InviteCodeStatus
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class CoupleInviteService(
    private val coupleRepository: CoupleRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val inviteCodeRepository: InviteCodeRepositoryPort,
    private val cacheManager: CacheManager,
    private val rateLimiter: CoupleInviteRateLimiter,
    private val coupleProfileImageUrlResolver: CoupleProfileImageUrlResolver,
    private val createCoupleUseCase: CreateCoupleUseCase,
) : CoupleInviteUseCase {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    @Transactional(readOnly = true)
    override fun getMyStatus(userId: Long): CoupleStatusReadModel {
        val couple = coupleRepository.findByUserId(userId) ?: return uncoupledStatusReadModel()
        if (couple.status != CoupleStatus.CONNECTED || !couple.isFull()) {
            return couple.toUncoupledStatusReadModel()
        }

        val partnerId = couple.partnerIdFor(userId) ?: return couple.toUncoupledStatusReadModel()
        val partner = userRepository.findById(partnerId) ?: throw BusinessException.UserNotFoundException()
        return couple.toCoupledStatusReadModel(partner)
    }

    @Transactional
    @RateLimit(
        key = "'invite:create:' + #userId",
        windowSeconds = 60,
        maxRequests = 5,
    )
    override fun generateInviteCode(userId: Long): InviteCodeIssueReadModel {
        validateIssuerReady(userId)

        val now = LocalDateTime.now()
        purgeExpiredUnusedInvites(userId, now)
        inviteCodeRepository.findActiveUnusedByUserIdForUpdate(userId, now).firstOrNull()?.let {
            return it.toIssueReadModel()
        }

        val created = issueNewInviteCode(userId, now)
        log.info(
            "invite_created inviterUserId={} inviteId={} expiresAt={}",
            userId,
            created.id,
            created.expiresAt,
        )
        return created.toIssueReadModel()
    }

    @Transactional
    @RateLimit(
        key = "'invite:create:' + #userId",
        windowSeconds = 60,
        maxRequests = 5,
    )
    override fun refreshInviteCode(userId: Long): InviteCodeIssueReadModel {
        validateIssuerReady(userId)

        val now = LocalDateTime.now()
        purgeExpiredUnusedInvites(userId, now)
        inviteCodeRepository
            .findActiveUnusedByUserIdForUpdate(userId, now)
            .forEach { inviteCodeRepository.deleteById(it.id) }

        val created = issueNewInviteCode(userId, now)
        log.info(
            "invite_created inviterUserId={} inviteId={} expiresAt={}",
            userId,
            created.id,
            created.expiresAt,
        )
        return created.toIssueReadModel()
    }

    @Transactional
    override fun revokeInviteCode(
        userId: Long,
        inviteCode: String,
    ) {
        val invite =
            inviteCodeRepository.findByCodeForUpdate(inviteCode)
                ?: throw BusinessException.InviteCodeNotFoundException()

        if (!invite.isOwnedBy(userId)) {
            throw BusinessException.InviteNotOwnerException()
        }
        if (invite.status == InviteCodeStatus.USED) {
            throw BusinessException.InviteAlreadyUsedException()
        }
        if (invite.status == InviteCodeStatus.UNUSED) {
            inviteCodeRepository.deleteById(invite.id)
            log.info("invite_revoked_deleted inviterUserId={} inviteId={}", userId, invite.id)
        }
    }

    @Transactional(readOnly = true)
    override fun verifyInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): InviteCodePreviewReadModel {
        rateLimiter.checkVerificationAllowed(userId, clientIp)
        if (!InviteCodePolicy.isValidFormat(inviteCode)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteInvalidFormatException()
        }
        validateNotCoupled(userId)

        val invite =
            inviteCodeRepository.findByCode(inviteCode)
                ?: run {
                    rateLimiter.recordVerificationFailure(userId, clientIp)
                    throw BusinessException.InviteCodeNotFoundException()
                }

        if (invite.isOwnedBy(userId)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.SelfInviteNotAllowedException()
        }
        validateInviteState(invite, userId, clientIp)

        rateLimiter.clearVerificationFailures(userId, clientIp)
        log.info("invite_verified verifierUserId={} inviteId={}", userId, invite.id)
        return invite.toPreviewReadModel(profileImageUrl = lockImageUrl())
    }

    @Transactional
    override fun joinByInviteCode(
        userId: Long,
        inviteCode: String,
        clientIp: String,
    ): CoupleStatusReadModel {
        findCurrentCoupledStatus(userId)?.let { return it }
        rateLimiter.checkVerificationAllowed(userId, clientIp)
        if (!InviteCodePolicy.isValidFormat(inviteCode)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteInvalidFormatException()
        }

        val invite =
            inviteCodeRepository.findByCodeForUpdate(inviteCode)
                ?: run {
                    rateLimiter.recordVerificationFailure(userId, clientIp)
                    throw BusinessException.InviteCodeNotFoundException()
                }

        val inviterId = invite.createdBy.userId
        if (inviterId == userId) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.SelfInviteNotAllowedException()
        }

        userRepository.lockByIds(listOf(userId, inviterId).distinct())

        if (isCoupled(userId) || isCoupled(inviterId)) {
            rateLimiter.recordVerificationFailure(userId, clientIp)
            throw BusinessException.InviteAlreadyCoupledException()
        }

        validateInviteState(invite, userId, clientIp)
        pruneIncompleteCoupleIfExists(userId)

        val inviterCouple =
            coupleRepository.findByUserId(inviterId)
                ?: throw BusinessException.InviteCodeNotFoundException()

        try {
            val joined = coupleRepository.addUser(inviterCouple.id, userId)
            inviteCodeRepository.deleteById(invite.id)

            cacheManager.evictUserCoupleCache(userId, inviterId)
            cacheManager.clearPermissionCaches()
            rateLimiter.clearVerificationFailures(userId, clientIp)
            updateProfileImageForRole(inviterId, CoupleProfileImage.LOCK)
            updateProfileImageForRole(userId, CoupleProfileImage.KEY)

            val partnerId = joined.partnerIdFor(userId) ?: throw BusinessException.UserNotFoundException()
            val partner = userRepository.findById(partnerId) ?: throw BusinessException.UserNotFoundException()
            log.info("couple_linked inviterUserId={} joinerUserId={} coupleId={}", inviterId, userId, joined.id)
            return joined.toCoupledStatusReadModel(partner.withProfileImage(lockImageUrl()))
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

    private fun findCurrentCoupledStatus(userId: Long): CoupleStatusReadModel? {
        val couple = coupleRepository.findByUserId(userId) ?: return null
        if (!couple.isConnectedAndFull()) return null
        val partnerId = couple.partnerIdFor(userId) ?: return null
        val partner = userRepository.findById(partnerId) ?: throw BusinessException.UserNotFoundException()
        return couple.toCoupledStatusReadModel(partner)
    }

    private fun validateIssuerReady(userId: Long) {
        val couple = coupleRepository.findByUserId(userId) ?: throw BusinessException.CoupleNotFoundException()
        if (couple.isConnectedAndFull()) {
            throw BusinessException.InviteAlreadyCoupledException()
        }
        if (couple.status != CoupleStatus.CONNECTED) {
            coupleRepository.removeCoupleUser(userId)
            cacheManager.evictUserCoupleCache(userId)
            createCoupleUseCase.createIfNone(Couple(name = Couple.DEFAULT_COUPLE_NAME), userId)
        }
    }

    private fun isCoupled(userId: Long): Boolean {
        val couple = coupleRepository.findByUserId(userId) ?: return false
        return couple.isConnectedAndFull()
    }

    private fun pruneIncompleteCoupleIfExists(userId: Long) {
        val existingCouple = coupleRepository.findByUserId(userId) ?: return
        val fullCouple = coupleRepository.findById(existingCouple.id) ?: return

        if (fullCouple.isConnectedAndFull()) {
            throw BusinessException.InviteAlreadyCoupledException(
                errors = errorDetailsOf(ErrorField.COUPLE_ID to existingCouple.id),
            )
        }
        coupleRepository.deleteById(existingCouple.id)
    }

    private fun validateInviteState(
        invite: InviteCode,
        userId: Long,
        clientIp: String,
    ) {
        when (invite.rejectionReason(LocalDateTime.now())) {
            InviteCodeRejectionReason.EXPIRED -> {
                inviteCodeRepository.deleteById(invite.id)
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeExpiredException()
            }

            InviteCodeRejectionReason.USED -> {
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeUsedException()
            }

            InviteCodeRejectionReason.REVOKED -> {
                rateLimiter.recordVerificationFailure(userId, clientIp)
                throw BusinessException.InviteCodeRevokedException()
            }

            null -> {
                Unit
            }
        }
    }

    private fun issueNewInviteCode(
        userId: Long,
        now: LocalDateTime,
    ): InviteCode {
        userRepository.findById(userId) ?: throw BusinessException.UserNotFoundException()

        repeat(InviteCodePolicy.RETRY_LIMIT) {
            val newCode = InviteCodePolicy.generateCode(secureRandom)
            if (!inviteCodeRepository.existsByCode(newCode)) {
                return inviteCodeRepository.createUnused(
                    code = newCode,
                    createdByUserId = userId,
                    expiresAt = now.plusHours(InviteCodePolicy.EXPIRATION_HOURS),
                )
            }
        }
        throw BusinessException.InviteRaceConflictException()
    }

    private fun purgeExpiredUnusedInvites(
        userId: Long,
        now: LocalDateTime,
    ) {
        inviteCodeRepository
            .findExpiredUnusedByUserIdForUpdate(userId, now)
            .forEach { inviteCodeRepository.deleteById(it.id) }
    }

    private fun updateProfileImageForRole(
        userId: Long,
        image: CoupleProfileImage,
    ) {
        val user = userRepository.findById(userId) ?: throw BusinessException.UserNotFoundException()
        val targetUrl = coupleProfileImageUrlResolver.resolve(image)
        if (user.profileImageUrl != targetUrl) {
            userRepository.update(user.withProfileImage(targetUrl))
        }
    }

    private fun lockImageUrl(): String = coupleProfileImageUrlResolver.resolve(CoupleProfileImage.LOCK)
}
