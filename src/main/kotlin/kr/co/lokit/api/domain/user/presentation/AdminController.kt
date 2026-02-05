package kr.co.lokit.api.domain.user.presentation

import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenJpaRepository
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("admin")
class AdminController(
    private val userJpaRepository: UserJpaRepository,
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
    private val coupleJpaRepository: CoupleJpaRepository,
    private val albumJpaRepository: AlbumJpaRepository,
    private val albumBoundsJpaRepository: AlbumBoundsJpaRepository,
    private val cacheManager: CacheManager,
    @Value("\${kakao.front-redirect-uri}") private val redirectUrl: String,
) {
    @GetMapping("delete/{email}")
    @Transactional
    fun deleteUser(
        @PathVariable email: String,
    ): ResponseEntity<Unit> {
        val user = userJpaRepository.findByEmail(email)
            ?: return ResponseEntity.notFound().build()
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

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl + "/login"))
            .build()
    }

    @GetMapping("cache/clear")
    fun clearAllCaches(): ResponseEntity<Unit> {
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(redirectUrl))
            .build()
    }
}
