package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsRepository
import kr.co.lokit.api.domain.map.infrastructure.ClusterProjection
import kr.co.lokit.api.domain.map.infrastructure.MapRepository
import kr.co.lokit.api.domain.map.infrastructure.PhotoProjection
import kr.co.lokit.api.domain.map.infrastructure.geocoding.MapClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class MapServiceTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Mock
    lateinit var mapRepository: MapRepository

    @Mock
    lateinit var albumBoundsRepository: AlbumBoundsRepository

    @Mock
    lateinit var mapClient: MapClient

    @InjectMocks
    lateinit var mapService: MapService

    @Test
    fun `줌 레벨이 15 미만이면 클러스터링된 결과를 반환한다`() {
        val clusters = listOf(
            ClusterProjection(
                cellX = 1L, cellY = 1L, count = 5,
                thumbnailUrl = "https://example.com/photo.jpg",
                centerLongitude = 127.0, centerLatitude = 37.5,
            ),
        )
        `when`(
            mapRepository.findClustersWithinBBox(
                west = anyDouble(), south = anyDouble(), east = anyDouble(), north = anyDouble(),
                gridSize = anyDouble(), albumId = isNull(),
            ),
        ).thenReturn(clusters)

        val result = mapService.getPhotos(12, BBox(126.9, 37.4, 127.1, 37.6))

        assertNotNull(result.clusters)
        assertEquals(1, result.clusters!!.size)
        assertEquals(5, result.clusters!![0].count)
    }

    @Test
    fun `줌 레벨이 15 이상이면 개별 사진을 반환한다`() {
        val photos = listOf(
            PhotoProjection(
                id = 1L, url = "https://example.com/photo.jpg",
                longitude = 127.0, latitude = 37.5,
                takenAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            ),
        )
        `when`(
            mapRepository.findPhotosWithinBBox(
                west = anyDouble(), south = anyDouble(), east = anyDouble(), north = anyDouble(),
                albumId = isNull(),
            ),
        ).thenReturn(photos)

        val result = mapService.getPhotos(15, BBox(126.9, 37.4, 127.1, 37.6))

        assertNotNull(result.photos)
        assertEquals(1, result.photos!!.size)
    }

    @Test
    fun `앨범 지도 정보를 조회할 수 있다`() {
        val bounds = AlbumBounds(
            id = 1L, albumId = 1L,
            minLongitude = 126.0, maxLongitude = 128.0,
            minLatitude = 37.0, maxLatitude = 38.0,
        )
        `when`(albumBoundsRepository.findByAlbumId(1L)).thenReturn(bounds)

        val result = mapService.getAlbumMapInfo(1L)

        assertEquals(1L, result.albumId)
        assertEquals(127.0, result.centerLongitude)
        assertEquals(37.5, result.centerLatitude)
        assertNotNull(result.boundingBox)
    }

    @Test
    fun `사진이 없는 앨범의 지도 정보는 null을 반환한다`() {
        `when`(albumBoundsRepository.findByAlbumId(1L)).thenReturn(null)

        val result = mapService.getAlbumMapInfo(1L)

        assertEquals(1L, result.albumId)
        assertNull(result.centerLongitude)
        assertNull(result.boundingBox)
    }

    @Test
    fun `위치 정보를 조회할 수 있다`() {
        `when`(mapClient.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )

        val result = mapService.getLocationInfo(127.0, 37.5)

        assertEquals("서울 강남구", result.address)
        assertEquals("역삼역", result.placeName)
    }
}
