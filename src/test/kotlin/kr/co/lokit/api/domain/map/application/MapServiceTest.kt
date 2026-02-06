package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.fixture.createAlbumBounds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class MapServiceTest {

    @Mock
    lateinit var mapQueryPort: MapQueryPort

    @Mock
    lateinit var albumBoundsRepository: AlbumBoundsRepositoryPort

    @Mock
    lateinit var albumRepository: AlbumRepositoryPort

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var mapClientPort: MapClientPort

    @Mock
    lateinit var transactionTemplate: TransactionTemplate

    @Mock
    lateinit var mapPhotosCacheService: MapPhotosCacheService

    @InjectMocks
    lateinit var mapService: MapQueryService

    @Test
    fun `줌 레벨이 15 미만이면 클러스터링된 결과를 반환한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val expectedResponse = MapPhotosResponse(
            clusters = listOf(
                ClusterResponse(
                    clusterId = "1:1",
                    count = 5,
                    thumbnailUrl = "https://example.com/photo.jpg",
                    longitude = 127.0,
                    latitude = 37.5,
                ),
            ),
        )

        `when`(
            mapPhotosCacheService.getClusteredPhotos(
                zoom = 12,
                bbox = bbox,
                coupleId = null,
                albumId = null,
            ),
        ).thenReturn(expectedResponse)

        val result = mapService.getPhotos(12, bbox, null, null)

        assertNotNull(result.clusters)
        assertEquals(1, result.clusters.size)
        assertEquals(5, result.clusters[0].count)
    }

    @Test
    fun `줌 레벨이 18 이상이면 개별 사진을 반환한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val expectedResponse = MapPhotosResponse(
            photos = listOf(
                MapPhotoResponse(
                    id = 1L,
                    thumbnailUrl = "https://example.com/photo.jpg",
                    longitude = 127.0,
                    latitude = 37.5,
                    takenAt = LocalDateTime.of(2026, 1, 1, 12, 0),
                ),
            ),
        )

        `when`(mapPhotosCacheService.buildCacheKey(18, bbox, null, null))
            .thenReturn("18:126900:37400")
        `when`(
            mapPhotosCacheService.getIndividualPhotos(
                bbox = bbox,
                coupleId = null,
                albumId = null,
                cacheKey = "18:126900:37400",
            ),
        ).thenReturn(expectedResponse)

        val result = mapService.getPhotos(18, bbox, null, null)

        assertNotNull(result.photos)
        assertEquals(1, result.photos.size)
    }

    @Test
    fun `앨범 지도 정보를 조회할 수 있다`() {
        val bounds = createAlbumBounds(
            id = 1L,
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
        `when`(mapClientPort.reverseGeocode(127.0, 37.5)).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )

        val result = mapService.getLocationInfo(127.0, 37.5)

        assertEquals("서울 강남구", result.address)
        assertEquals("역삼역", result.placeName)
    }
}
