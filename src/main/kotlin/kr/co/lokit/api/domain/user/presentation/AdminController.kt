package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.cache.clearAllCaches
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.couple.infrastructure.CoupleUserEntity
import kr.co.lokit.api.domain.couple.presentation.mapping.toResponse
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsJpaRepository
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.AdminActionResponse
import kr.co.lokit.api.domain.user.dto.AdminInviteActionRequest
import kr.co.lokit.api.domain.user.dto.AdminInviteIssueRequest
import kr.co.lokit.api.domain.user.dto.AdminCoupleLinkRequest
import kr.co.lokit.api.domain.user.dto.AdminCoupleLinkResponse
import kr.co.lokit.api.domain.user.dto.AdminUserSummaryResponse
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.mapping.toDomain
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("admin")
class AdminController(
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val albumBoundsJpaRepository: AlbumBoundsJpaRepository,
    private val coupleInviteUseCase: CoupleInviteUseCase,
    private val jwtTokenProvider: JwtTokenProvider,
    private val cacheManager: CacheManager,
    @Value("\${admin.key}") private val adminKey: String,
) : AdminApi {
    companion object {
        private const val DEFAULT_COUPLE_NAME = "테스트 커플"
        private const val ADMIN_TEST_CLIENT_IP = "127.0.0.1"
    }

    @Operation(
        summary = "모든 유저의 DB 식별자와 Email을 조회합니다.",
    )
    @GetMapping("users")
    override fun getUsers(
        @RequestHeader("X-Admin-Key") key: String,
    ): List<AdminUserSummaryResponse> {
        validateAdminKey(key)
        return userJpaRepository.findAll().map { user ->
            AdminUserSummaryResponse(
                id = user.nonNullId(),
                email = user.email,
            )
        }
    }

    @Operation(
        summary = "Email에 해당하는 유저의 모든 데이터를 삭제합니다.",
    )
    @DeleteMapping("users/{email}")
    @Transactional
    override fun deleteAllByEmail(
        @PathVariable email: String,
        @RequestHeader("X-Admin-Key") key: String,
    ): AdminActionResponse {
        validateAdminKey(key)
        val user =
            userJpaRepository.findByEmail(email)
                ?: throw BusinessException.UserNotFoundException(message = "User not found for email: $email")
        val userId = user.nonNullId()

        refreshTokenRepository.deleteByUserId(userId)

        val couple = coupleJpaRepository.findByUserId(userId)
        if (couple != null) {
            val albumIds = albumJpaRepository.findAlbumIdsByCoupleId(couple.nonNullId())
            if (albumIds.isNotEmpty()) {
                albumBoundsJpaRepository.deleteAllByStandardIdIn(albumIds)
            }
            coupleJpaRepository.delete(couple)
        }

        userJpaRepository.delete(user)
        cacheManager.clearAllCaches()

        return AdminActionResponse(message = "삭제 완료: $email")
    }

    @Operation(
        summary = "서버 전체의 캐시 데이터를 강제 만료합니다.",
    )
    @PostMapping("cache/clear")
    override fun clearAllCaches(
        @RequestHeader("X-Admin-Key") key: String,
    ): AdminActionResponse {
        validateAdminKey(key)
        cacheManager.clearAllCaches()
        return AdminActionResponse(message = "캐시 초기화 완료")
    }

    @PostMapping("couple/test-link")
    @Transactional
    override fun linkTestCouple(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: AdminCoupleLinkRequest,
    ): AdminCoupleLinkResponse {
        val primary =
            userJpaRepository.findById(userId).orElseThrow {
                BusinessException.UserNotFoundException(message = "User not found for id: $userId")
            }
        val partnerEmail =
            request.partnerEmail
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "${primary.nonNullId()}.pair@lokit.local"

        if (primary.email.equals(partnerEmail, ignoreCase = true)) {
            throw BusinessException.InvalidInputException(message = "primaryEmail과 partnerEmail은 달라야 합니다.")
        }
        val partner = findOrCreateUserByEmail(partnerEmail)

        val linkedCouple =
            linkUsers(
                primary = primary,
                partner = partner,
                coupleName = request.coupleName?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_COUPLE_NAME,
            )
        val partnerTokens = issueTokens(partner)
        cacheManager.clearAllCaches()

        val action =
            when {
                linkedCouple.coupleUsers.any { it.user.nonNullId() == primary.nonNullId() } &&
                    linkedCouple.coupleUsers.any { it.user.nonNullId() == partner.nonNullId() } -> "LINKED"
                else -> "UNCHANGED"
            }

        return AdminCoupleLinkResponse(
            coupleId = linkedCouple.nonNullId(),
            primaryUserId = primary.nonNullId(),
            primaryEmail = primary.email,
            partnerUserId = partner.nonNullId(),
            partnerEmail = partner.email,
            action = action,
            partnerAccessToken = partnerTokens.first,
            partnerRefreshToken = partnerTokens.second,
        )
    }

    private fun validateAdminKey(key: String) {
        if (!adminKey.trim().equals(key.trim(), ignoreCase = true)) {
            throw BusinessException.ForbiddenException(message = "관리자 키가 유효하지 않습니다.")
        }
    }

    private fun findOrCreateUserByEmail(email: String): UserEntity =
        userJpaRepository.findByEmail(email)
            ?: userJpaRepository.save(
                UserEntity(
                    email = email,
                    name = User.defaultNicknameFor(email),
                ),
            )

    private fun linkUsers(
        primary: UserEntity,
        partner: UserEntity,
        coupleName: String,
    ): CoupleEntity {
        val primaryCouple = coupleJpaRepository.findByUserId(primary.nonNullId())
        val partnerCouple = coupleJpaRepository.findByUserId(partner.nonNullId())

        if (primaryCouple != null && partnerCouple != null) {
            if (primaryCouple.nonNullId() == partnerCouple.nonNullId()) {
                return primaryCouple
            }
            throw BusinessException.BusinessRuleViolationException(
                message = "두 사용자가 서로 다른 커플에 이미 연결되어 있습니다.",
            )
        }

        if (primaryCouple != null) {
            return attachUser(primaryCouple, partner)
        }
        if (partnerCouple != null) {
            return attachUser(partnerCouple, primary)
        }
        return createCoupleWithUsers(primary, partner, coupleName)
    }

    private fun attachUser(
        couple: CoupleEntity,
        user: UserEntity,
    ): CoupleEntity {
        if (couple.coupleUsers.any { it.user.nonNullId() == user.nonNullId() }) {
            return couple
        }
        if (couple.coupleUsers.size >= 2) {
            throw BusinessException.BusinessRuleViolationException(message = "대상 커플이 이미 가득 찼습니다.")
        }
        couple.addUser(CoupleUserEntity(couple = couple, user = user))
        return couple
    }

    private fun createCoupleWithUsers(
        primary: UserEntity,
        partner: UserEntity,
        coupleName: String,
    ): CoupleEntity {
        val couple = coupleJpaRepository.save(CoupleEntity(name = coupleName))
        couple.addUser(CoupleUserEntity(couple = couple, user = primary))
        couple.addUser(CoupleUserEntity(couple = couple, user = partner))
        albumJpaRepository.save(
            kr.co.lokit.api.domain.album.infrastructure.AlbumEntity(
                title = "전체보기",
                couple = couple,
                createdBy = primary,
                isDefault = true,
            ),
        )
        return couple
    }

    private fun issueTokens(user: UserEntity): Pair<String, String> {
        val accessToken = jwtTokenProvider.generateAccessToken(user.toDomain())
        val refreshToken = jwtTokenProvider.generateRefreshToken()
        val expiresAt =
            LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMillis() / 1000,
            )
        refreshTokenRepository.replace(
            userId = user.nonNullId(),
            token = refreshToken,
            expiresAt = expiresAt,
        )
        return accessToken to refreshToken
    }

    @PostMapping("couples/invites/issue")
    override fun issueInviteCode(
        @RequestHeader("X-Admin-Key") key: String,
        @Valid @RequestBody request: AdminInviteIssueRequest,
    ): InviteCodeResponse {
        validateAdminKey(key)
        val issuer = userJpaRepository.findByEmail(request.issuerEmail.trim()) ?: findOrCreateUserByEmail(request.issuerEmail.trim())
        return coupleInviteUseCase.generateInviteCode(issuer.nonNullId()).toResponse()
    }

    @PostMapping("couples/invites/verify")
    override fun verifyInviteCode(
        @RequestHeader("X-Admin-Key") key: String,
        @Valid @RequestBody request: AdminInviteActionRequest,
    ): InviteCodePreviewResponse {
        validateAdminKey(key)
        val actor = userJpaRepository.findByEmail(request.actorEmail.trim()) ?: findOrCreateUserByEmail(request.actorEmail.trim())
        return coupleInviteUseCase
            .verifyInviteCode(
                userId = actor.nonNullId(),
                inviteCode = request.inviteCode,
                clientIp = ADMIN_TEST_CLIENT_IP,
            ).toResponse()
    }

    @PostMapping("couples/invites/confirm")
    override fun confirmInviteCode(
        @RequestHeader("X-Admin-Key") key: String,
        @Valid @RequestBody request: AdminInviteActionRequest,
    ): CoupleStatusResponse {
        validateAdminKey(key)
        val actor = userJpaRepository.findByEmail(request.actorEmail.trim()) ?: findOrCreateUserByEmail(request.actorEmail.trim())
        return coupleInviteUseCase
            .confirmInviteCode(
                userId = actor.nonNullId(),
                inviteCode = request.inviteCode,
                clientIp = ADMIN_TEST_CLIENT_IP,
            ).toResponse()
    }
}
