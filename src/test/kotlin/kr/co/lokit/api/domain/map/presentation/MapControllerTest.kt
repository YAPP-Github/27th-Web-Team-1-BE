package kr.co.lokit.api.domain.map.presentation

import kr.co.lokit.api.config.security.CompositeAuthenticationResolver
import kr.co.lokit.api.domain.map.application.MapService
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.map.dto.BoundingBoxResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MapController::class)
class MapControllerTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var compositeAuthenticationResolver: CompositeAuthenticationResolver

    @MockitoBean
    lateinit var mapService: MapService

    @Test
    fun `지도 사진 조회 성공`() {
        doReturn(MapPhotosResponse(clusters = emptyList()))
            .`when`(mapService).getPhotos(anyInt(), anyObject(), anyObject())

        mockMvc.perform(
            get("/map/photos")
                .with(user("test").roles("USER"))
                .param("zoom", "12")
                .param("bbox", "126.9,37.4,127.1,37.6"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `앨범 지도 정보 조회 성공`() {
        val response = AlbumMapInfoResponse(
            albumId = 1L,
            centerLongitude = 127.0,
            centerLatitude = 37.5,
            boundingBox = BoundingBoxResponse(west = 126.0, south = 37.0, east = 128.0, north = 38.0),
        )
        doReturn(response).`when`(mapService).getAlbumMapInfo(1L)

        mockMvc.perform(
            get("/map/albums/1")
                .with(user("test").roles("USER")),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `위치 정보 조회 성공`() {
        doReturn(LocationInfoResponse(address = "서울 강남구", placeName = null, regionName = "강남구"))
            .`when`(mapService).getLocationInfo(anyDouble(), anyDouble())

        mockMvc.perform(
            get("/map/location")
                .with(user("test").roles("USER"))
                .param("lng", "127.0")
                .param("lat", "37.5"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `장소 검색 성공`() {
        doReturn(PlaceSearchResponse(places = emptyList()))
            .`when`(mapService).searchPlaces(anyString())

        mockMvc.perform(
            get("/map/places/search")
                .with(user("test").roles("USER"))
                .param("query", "스타벅스"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `인증되지 않은 사용자는 접근할 수 없다`() {
        mockMvc.perform(get("/map/photos").param("zoom", "12").param("bbox", "126.9,37.4,127.1,37.6"))
            .andExpect(status().isUnauthorized)
    }
}
