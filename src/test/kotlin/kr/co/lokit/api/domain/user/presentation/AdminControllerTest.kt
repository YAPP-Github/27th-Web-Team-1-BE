package kr.co.lokit.api.domain.user.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.album.infrastructure.AlbumJpaRepository
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.infrastructure.CoupleJpaRepository
import kr.co.lokit.api.domain.couple.domain.InviteCodeIssueReadModel
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsJpaRepository
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.port.RefreshTokenRepositoryPort
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.fixture.createAlbumEntity
import kr.co.lokit.api.fixture.createCoupleEntity
import kr.co.lokit.api.fixture.createUserEntity
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminController::class)
@TestPropertySource(properties = ["admin.key=test-admin-key"])
class AdminControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper: ObjectMapper = ObjectMapper()

    @MockitoBean
    lateinit var compositeAuthenticationResolver: CompositeAuthenticationResolver

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var cookieProperties: CookieProperties

    @MockitoBean
    lateinit var cookieGenerator: CookieGenerator

    @MockitoBean
    lateinit var userJpaRepository: UserJpaRepository

    @MockitoBean
    lateinit var refreshTokenRepository: RefreshTokenRepositoryPort

    @MockitoBean
    lateinit var coupleJpaRepository: CoupleJpaRepository

    @MockitoBean
    lateinit var coupleInviteUseCase: CoupleInviteUseCase

    @MockitoBean
    lateinit var albumJpaRepository: AlbumJpaRepository

    @MockitoBean
    lateinit var albumBoundsJpaRepository: AlbumBoundsJpaRepository

    @MockitoBean
    lateinit var cacheManager: CacheManager

    @Test
    fun `users 조회 성공`() {
        doReturn(listOf(createUserEntity(id = 1L, email = "dev@example.com"))).`when`(userJpaRepository).findAll()

        mockMvc
            .perform(
                get("/admin/users")
                    .with(authentication(userAuth()))
                    .header("X-Admin-Key", "test-admin-key"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1L))
            .andExpect(jsonPath("$.data[0].email").value("dev@example.com"))
    }

    @Test
    fun `admin key 불일치면 403`() {
        mockMvc
            .perform(
                get("/admin/users")
                    .with(authentication(userAuth()))
                    .header("X-Admin-Key", "wrong-key"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `개발용 커플 연결 생성 성공`() {
        val primary = createUserEntity(id = 1L, email = "dev@example.com", name = "dev")
        val partner = createUserEntity(id = 2L, email = "1.pair@lokit.local", name = "pair")
        val couple = createCoupleEntity(id = 10L, name = "테스트 커플")

        doReturn(java.util.Optional.of(primary)).`when`(userJpaRepository).findById(1L)
        doReturn(null).`when`(userJpaRepository).findByEmail("1.pair@lokit.local")
        doReturn(partner).`when`(userJpaRepository).save(any())
        doReturn(null).`when`(coupleJpaRepository).findByUserId(1L)
        doReturn(null).`when`(coupleJpaRepository).findByUserId(2L)
        doReturn(couple).`when`(coupleJpaRepository).save(any())
        doReturn(createAlbumEntity(id = 100L, couple = couple, createdBy = primary)).`when`(albumJpaRepository).save(any())
        doReturn(emptySet<String>()).`when`(cacheManager).cacheNames
        whenever(jwtTokenProvider.generateAccessToken(org.mockito.kotlin.any())).thenReturn("partner-access-token")
        whenever(jwtTokenProvider.generateRefreshToken()).thenReturn("partner-refresh-token")
        whenever(jwtTokenProvider.getRefreshTokenExpirationMillis()).thenReturn(604800000L)

        mockMvc
            .perform(
                post("/admin/couple/test-link")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "coupleName" to "테스트 커플",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.coupleId").value(10L))
            .andExpect(jsonPath("$.data.primaryEmail").value("dev@example.com"))
            .andExpect(jsonPath("$.data.partnerEmail").value("1.pair@lokit.local"))
            .andExpect(jsonPath("$.data.action").value("LINKED"))
            .andExpect(jsonPath("$.data.partnerAccessToken").value("partner-access-token"))
            .andExpect(jsonPath("$.data.partnerRefreshToken").value("partner-refresh-token"))

        verify(coupleJpaRepository).save(any())
    }

    @Test
    fun `캐시 초기화 성공`() {
        val cache = org.mockito.Mockito.mock(Cache::class.java)
        doReturn(setOf("mapPhotos")).`when`(cacheManager).cacheNames
        doReturn(cache).`when`(cacheManager).getCache(eq("mapPhotos"))

        mockMvc
            .perform(
                post("/admin/cache/clear")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .header("X-Admin-Key", "test-admin-key"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.message").value("캐시 초기화 완료"))

        verify(cache).clear()
    }

    @Test
    fun `개발용 초대코드 발급 성공`() {
        val issuer = createUserEntity(id = 1L, email = "dev@example.com")
        doReturn(issuer).`when`(userJpaRepository).findByEmail("dev@example.com")
        doReturn(InviteCodeIssueReadModel(inviteCode = "123456", expiresAt = LocalDateTime.of(2026, 1, 1, 0, 0)))
            .`when`(coupleInviteUseCase).generateInviteCode(1L)

        mockMvc
            .perform(
                post("/admin/couples/invites/issue")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .header("X-Admin-Key", "test-admin-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"issuerEmail":"dev@example.com"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.inviteCode").value("123456"))
    }

    @Test
    fun `사용자 전체 삭제 시 존재하지 않는 이메일이면 404`() {
        doReturn(null).`when`(userJpaRepository).findByEmail("missing@example.com")

        mockMvc
            .perform(
                delete("/admin/users/missing@example.com")
                    .with(authentication(userAuth()))
                    .with(csrf())
                    .header("X-Admin-Key", "test-admin-key"),
            ).andExpect(status().isNotFound)
    }
}
