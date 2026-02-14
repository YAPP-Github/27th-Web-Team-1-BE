package kr.co.lokit.api.domain.map.presentation

import kr.co.lokit.api.common.permission.PermissionService
import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.config.security.JwtTokenProvider
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.config.web.CookieProperties
import kr.co.lokit.api.domain.map.application.port.`in`.GetMapUseCase
import kr.co.lokit.api.domain.map.application.port.`in`.SearchLocationUseCase
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.BoundingBoxResponse
import kr.co.lokit.api.domain.map.dto.HomeResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapMeResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.fixture.userAuth
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.kotlin.verify
import org.mockito.kotlin.eq

@WebMvcTest(MapController::class)
class MapControllerTest {
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Autowired
    lateinit var mockMvc: MockMvc

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
    lateinit var getMapUseCase: GetMapUseCase

    @MockitoBean
    lateinit var searchLocationUseCase: SearchLocationUseCase

    @MockitoBean
    lateinit var permissionService: PermissionService

    private fun createMapMeResponse(): MapMeResponse =
        MapMeResponse(
            location = LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"),
            boundingBox = BoundingBoxResponse(west = 126.9, south = 37.4, east = 127.1, north = 37.6),
            totalHistoryCount = 0,
            albums = emptyList<HomeResponse.Companion.AlbumThumbnails>(),
            dataVersion = 1L,
            clusters = emptyList(),
            photos = null,
        )

    @Test
    fun `앨범 지도 정보 조회 성공`() {
        val response =
            AlbumMapInfoResponse(
                albumId = 1L,
                centerLongitude = 127.0,
                centerLatitude = 37.5,
                boundingBox = BoundingBoxResponse(west = 126.0, south = 37.0, east = 128.0, north = 38.0),
            )
        doReturn(response).`when`(getMapUseCase).getAlbumMapInfo(anyLong())

        mockMvc
            .perform(
                get("/map/albums/1")
                    .with(authentication(userAuth())),
            ).andExpect(status().isOk)
    }

    @Test
    fun `위치 정보 조회 성공`() {
        doReturn(LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"))
            .`when`(searchLocationUseCase)
            .getLocationInfo(anyDouble(), anyDouble())

        mockMvc
            .perform(
                get("/map/location")
                    .with(authentication(userAuth()))
                    .param("longitude", "127.0")
                    .param("latitude", "37.5"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `장소 검색 성공`() {
        doReturn(PlaceSearchResponse(places = emptyList()))
            .`when`(searchLocationUseCase)
            .searchPlaces(anyString())

        mockMvc
            .perform(
                get("/map/places/search")
                    .with(authentication(userAuth()))
                    .param("query", "스타벅스"),
            ).andExpect(status().isOk)
    }

    @Test
    fun `지도 ME v1_1 조회 성공`() {
        doReturn(createMapMeResponse()).`when`(getMapUseCase).getMe(anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyObject(), anyObject())

        mockMvc
            .perform(
                get("/map/me")
                    .with(authentication(userAuth()))
                    .header("X-API-VERSION", "1.1")
                    .param("west", "126.9")
                    .param("south", "37.4")
                    .param("east", "127.1")
                    .param("north", "37.6")
                    .param("zoom", "12.0"),
            ).andExpect(status().isOk)

        verify(getMapUseCase).getMe(eq(1L), eq(126.9), eq(37.4), eq(127.1), eq(37.6), eq(12.0), eq(null), eq(null))
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc
            .perform(get("/map/me").param("zoom", "12").param("long", "126.9,37.4").param("lat", "127.1,37.6"))
            .andExpect(status().isUnauthorized)
    }
}
