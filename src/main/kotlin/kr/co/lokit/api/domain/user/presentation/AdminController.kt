package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("admin")
class AdminController(
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val albumBoundsJpaRepository: AlbumBoundsJpaRepository,
    private val cacheManager: CacheManager,
    @Value("\${admin.key}") private val adminKey: String,
) {
    @Operation(
        summary = "모든 유저의 DB 식별자와 Email을 조회합니다.",
    )
    @SecurityRequirements
    @GetMapping("users")
    fun getUsers(
        @RequestHeader("X-Admin-Key") key: String,
    ): List<Pair<Long, String>> {
        if (!adminKey.trim().equals(key.trim(), ignoreCase = true)) {
            return listOf(Pair(-1L, "당신은 이 서버를 해킹할 자격이 없습니다. 안타깝네요 ㅠ0ㅠ"))
        }
        return userJpaRepository.findAll().map { user -> Pair(user.id!!, user.email) }
    }

    @Operation(
        summary = "Email에 해당하는 유저의 모든 데이터를 삭제합니다.",
    )
    @GetMapping("delete/{email}")
    @SecurityRequirements
    @Transactional
    fun deleteAllByEmail(
        @PathVariable email: String,
        @RequestHeader("X-Admin-Key") key: String,
    ): String {
        if (!adminKey.trim().equals(key.trim(), ignoreCase = true)) {
            return "당신은 이 서버를 해킹할 자격이 없습니다. 안타깝네요 ㅠ0ㅠ"
        }
        val user =
            userJpaRepository.findByEmail(email)
                ?: return "User not found for email: $email"
        val userId = user.nonNullId()

        // 1. 리프레시 토큰 삭제
        refreshTokenJpaRepository.deleteByUser(user)

        // 2. 커플 및 하위 데이터 삭제 (cascade: coupleUsers, albums → photos)
        val couple = coupleJpaRepository.findByUserId(userId)
        if (couple != null) {
            val albumIds = albumJpaRepository.findAlbumIdsByCoupleId(couple.nonNullId())
            if (albumIds.isNotEmpty()) {
                albumBoundsJpaRepository.deleteAllByStandardIdIn(albumIds)
            }
            coupleJpaRepository.delete(couple)
        }

        // 3. 유저 삭제
        userJpaRepository.delete(user)

        // 4. 캐시 전체 무효화
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }

        return "해킹에 성공하셨습니다. 축하합니다."
    }

    @Operation(
        summary = "서버 전체의 캐시 데이터를 강제 만료합니다.",
    )
    @SecurityRequirements
    @GetMapping("cache/clear")
    fun clearAllCaches(
        @RequestHeader("X-Admin-Key") key: String,
    ): String {
        if (!adminKey.trim().equals(key.trim(), ignoreCase = true)) {
            return "당신은 이 서버를 해킹할 자격이 없습니다. 안타깝네요 ㅠ0ㅠ"
        }
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
        return "해킹에 성공하셨습니다. 축하합니다."
    }
}
