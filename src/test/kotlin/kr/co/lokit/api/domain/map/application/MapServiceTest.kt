package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createAlbumBounds
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    lateinit var mapService: MapQueryService

    @BeforeEach
    fun setUp() {
        mapService =
            MapQueryService(
                mapQueryPort,
                albumBoundsRepository,
                albumRepository,
                coupleRepository,
                mapClientPort,
                mapPhotosCacheService,
            )
    }

    @Test
    fun `줌 레벨이 17 미만이면 클러스터링된 결과를 반환한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val expectedResponse =
            MapPhotosResponse(
                clusters =
                    listOf(
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
    fun `한국 경계 밖 bbox 요청은 빈 결과를 반환한다`() {
        val outsideKorea = BBox(-10.0, -10.0, -5.0, -5.0)

        val result = mapService.getPhotos(12, outsideKorea, null, null)

        assertNotNull(result.clusters)
        assertTrue(result.clusters.isEmpty())
        verifyNoInteractions(mapPhotosCacheService)
    }

    @Test
    fun `앨범 지도 정보를 조회할 수 있다`() {
        val bounds =
            createAlbumBounds(
                id = 1L,
                minLongitude = 126.0,
                maxLongitude = 128.0,
                minLatitude = 37.0,
                maxLatitude = 38.0,
            )
        `when`(albumBoundsRepository.findByStandardIdAndIdType(1L, BoundsIdType.ALBUM)).thenReturn(bounds)

        val result = mapService.getAlbumMapInfo(1L)

        assertEquals(1L, result.albumId)
        assertEquals(127.0, result.centerLongitude)
        assertEquals(37.5, result.centerLatitude)
        assertNotNull(result.boundingBox)
    }

    @Test
    fun `사진이 없는 앨범의 지도 정보는 null을 반환한다`() {
        `when`(albumBoundsRepository.findByStandardIdAndIdType(1L, BoundsIdType.ALBUM)).thenReturn(null)

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

    @Test
    fun `getClusterPhotos는 클러스터 ID를 파싱하여 사진을 조회한다`() {
        val photos =
            listOf(
                ClusterPhotoProjection(
                    id = 1L,
                    url = "https://example.com/photo1.jpg",
                    longitude = 127.0,
                    latitude = 37.5,
                    takenAt = LocalDateTime.of(2025, 1, 1, 12, 0),
                    address = "서울 강남구",
                ),
            )
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(
            mapQueryPort.findPhotosInGridCell(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                any(),
            ),
        ).thenReturn(photos)

        val result = mapService.getClusterPhotos("z14_24661_7867", 1L)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `searchPlaces는 mapClientPort에 위임한다`() {
        val places =
            listOf(
                PlaceResponse(
                    placeName = "스타벅스 강남역점",
                    address = "역삼동 858",
                    roadAddress = "강남대로 396",
                    longitude = 127.0,
                    latitude = 37.5,
                    category = "카페",
                ),
            )
        `when`(mapClientPort.searchPlaces("스타벅스")).thenReturn(places)

        val result = mapService.searchPlaces("스타벅스")

        assertEquals(1, result.places.size)
        assertEquals("스타벅스 강남역점", result.places[0].placeName)
        verify(mapClientPort).searchPlaces("스타벅스")
    }

    @Test
    fun `기본 앨범 요청의 dataVersion 계산에는 정규화된 albumId를 사용한다`() {
        val defaultAlbumId = 10L
        val currentVersion = 7L
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(albumRepository.findById(defaultAlbumId)).thenReturn(createAlbum(id = defaultAlbumId, isDefault = true))
        `when`(mapPhotosCacheService.getVersion(anyInt(), any(), eq(1L), isNull())).thenReturn(currentVersion)
        `when`(mapClientPort.reverseGeocode(anyDouble(), anyDouble())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(emptyList())
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        val result =
            mapService.getMe(
                userId = 1L,
                longitude = 127.0,
                latitude = 37.5,
                zoom = 14,
                bbox = BBox(126.9, 37.4, 127.1, 37.6),
                albumId = defaultAlbumId,
                lastDataVersion = currentVersion,
            )

        assertEquals(currentVersion, result.dataVersion)
        verify(mapPhotosCacheService).getVersion(anyInt(), any(), eq(1L), isNull())
    }
}
