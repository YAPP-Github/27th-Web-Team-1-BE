package kr.co.lokit.api.domain.user.presentation

import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.config.cache.clearAllCaches
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.application.CoupleCommandService
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsJpaRepository
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.domain.AuthTokens
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.AdminActionResponse
import kr.co.lokit.api.domain.user.dto.AdminPartnerResponse
import kr.co.lokit.api.domain.user.dto.AdminUserSummaryResponse
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.mapping.toDomain
import kr.co.lokit.api.domain.user.infrastructure.mapping.toEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("admin")
class AdminController(
    private val userJpaRepository: UserJpaRepository,
    private val createCoupleUseCase: CoupleCommandService,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val albumBoundsJpaRepository: AlbumBoundsJpaRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val cacheManager: CacheManager,
    @Value("\${admin.key}") private val adminKey: String,
) : AdminApi {
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

    @PostMapping("cache/clear")
    override fun clearAllCaches(
        @RequestHeader("X-Admin-Key") key: String,
    ): AdminActionResponse {
        validateAdminKey(key)
        cacheManager.clearAllCaches()
        return AdminActionResponse(message = "캐시 초기화 완료")
    }

    private fun validateAdminKey(key: String) {
        if (!adminKey.trim().equals(key.trim(), ignoreCase = true)) {
            throw BusinessException.ForbiddenException(message = "관리자 키가 유효하지 않습니다.")
        }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("couples/partner")
    @Transactional
    override fun createCouplePartner(
        @CurrentUserId userId: Long,
    ): AdminPartnerResponse {
        val primaryUser =
            userJpaRepository.findById(userId).orElseThrow {
                BusinessException.UserNotFoundException(message = "Primary user not found for ID: $userId")
            }

        val email = "dev.partner.${primaryUser.nonNullId()}@example.com"

        val partnerUser =
            userJpaRepository
                .save(
                    userJpaRepository.findByEmail(email) ?: userJpaRepository.save(
                        userJpaRepository.save(
                            User(
                                email = email,
                                name = "test-${primaryUser.nonNullId()}",
                            ).toEntity(),
                        ),
                    ),
                ).toDomain()

        createCoupleUseCase.createIfNone(Couple(name = Couple.DEFAULT_COUPLE_NAME), partnerUser.id)

        val accessToken = jwtTokenProvider.generateAccessToken(partnerUser)
        val refreshToken = jwtTokenProvider.generateRefreshToken()

        val expiresAt =
            LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMillis() / 1000,
            )

        refreshTokenRepository.replace(
            userId = partnerUser.id,
            token = refreshToken,
            expiresAt = expiresAt,
        )

        return AdminPartnerResponse(
            partnerEmail = email,
            tokens =
                AuthTokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                ),
        )
    }
}
