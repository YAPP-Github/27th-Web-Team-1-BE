package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createAlbumBounds
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
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

    @Mock
    lateinit var clusterBoundaryMergeStrategy: ClusterBoundaryMergeStrategy

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
                clusterBoundaryMergeStrategy,
            )
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
    fun `기본 앨범의 지도 정보 조회는 couple 기준 bounds를 사용한다`() {
        val albumId = 1L
        val coupleId = 99L
        val defaultAlbum = createAlbum(id = albumId, coupleId = coupleId, isDefault = true)
        val bounds = createAlbumBounds(id = 10L)
        `when`(albumRepository.findById(albumId)).thenReturn(defaultAlbum)
        `when`(albumBoundsRepository.findByStandardIdAndIdType(coupleId, BoundsIdType.COUPLE)).thenReturn(bounds)

        val result = mapService.getAlbumMapInfo(albumId)

        assertEquals(albumId, result.albumId)
        assertNotNull(result.boundingBox)
        verify(albumBoundsRepository).findByStandardIdAndIdType(coupleId, BoundsIdType.COUPLE)
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
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
            ),
        ).thenReturn(photos)
        `when`(
            clusterBoundaryMergeStrategy.resolveClusterCells(
                any(),
                any(),
                any(),
            ),
        ).thenAnswer { invocation ->
            val photosByCell: Map<CellCoord, List<GeoPoint>> = invocation.getArgument(1)
            photosByCell.keys
        }

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
        `when`(mapPhotosCacheService.getDataVersion(any(), any(), eq(1L), anyOrNull())).thenReturn(currentVersion)
        `when`(mapPhotosCacheService.getClusteredPhotos(any(), any(), eq(1L), anyOrNull(), any())).thenReturn(
            MapPhotosResponse(clusters = emptyList()),
        )
        `when`(mapClientPort.reverseGeocode(any(), any())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(emptyList())
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        val result =
            mapService.getMe(
                userId = 1L,
                longitude = 127.0,
                latitude = 37.5,
                zoom = 14.0,
                albumId = defaultAlbumId,
                lastDataVersion = currentVersion,
            )

        assertEquals(currentVersion, result.dataVersion)
        verify(mapPhotosCacheService).getDataVersion(any(), any(), eq(1L), anyOrNull())
        verify(mapPhotosCacheService).getClusteredPhotos(any(), any(), eq(1L), anyOrNull(), eq(true))
    }

    @Test
    fun `lastDataVersion이 다르면 셀 캐시 재사용 없이 전체 셀을 재조회한다`() {
        val currentVersion = 9L
        val staleVersion = 8L
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(mapPhotosCacheService.getDataVersion(any(), any(), eq(1L), anyOrNull())).thenReturn(currentVersion)
        `when`(mapPhotosCacheService.getClusteredPhotos(any(), any(), eq(1L), anyOrNull(), any())).thenReturn(
            MapPhotosResponse(clusters = emptyList()),
        )
        `when`(mapClientPort.reverseGeocode(any(), any())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(emptyList())
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        mapService.getMe(
            userId = 1L,
            longitude = 127.0,
            latitude = 37.5,
            zoom = 14.0,
            albumId = null,
            lastDataVersion = staleVersion,
        )

        verify(mapPhotosCacheService).getClusteredPhotos(any(), any(), eq(1L), anyOrNull(), eq(false))
    }

    @Test
    fun `getMe는 zoom 이상에서 개별 사진 조회 경로를 사용한다`() {
        val zoom = GridValues.CLUSTER_ZOOM_THRESHOLD
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(mapPhotosCacheService.getDataVersion(any(), any(), eq(1L), anyOrNull())).thenReturn(3L)
        `when`(mapPhotosCacheService.getIndividualPhotos(any(), any(), eq(1L), anyOrNull())).thenReturn(
            MapPhotosResponse(photos = emptyList()),
        )
        `when`(mapClientPort.reverseGeocode(any(), any())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(emptyList())
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        mapService.getMe(
            userId = 1L,
            longitude = 127.0,
            latitude = 37.5,
            zoom = zoom.toDouble(),
            albumId = null,
            lastDataVersion = 3L,
        )

        verify(mapPhotosCacheService).getIndividualPhotos(any(), any(), eq(1L), anyOrNull())
    }

    @Test
    fun `연결되지 않은 유저의 getMe는 빈 사진 응답을 반환한다`() {
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)
        `when`(mapPhotosCacheService.getDataVersion(any(), any(), anyOrNull(), anyOrNull())).thenReturn(0L)
        `when`(mapClientPort.reverseGeocode(any(), any())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        val result =
            mapService.getMe(
                userId = 1L,
                longitude = 127.0,
                latitude = 37.5,
                zoom = 14.0,
                albumId = null,
                lastDataVersion = null,
            )

        assertNotNull(result.clusters)
        assertTrue(result.clusters!!.isEmpty())
        verify(mapPhotosCacheService, org.mockito.Mockito.never()).getClusteredPhotos(
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            any(),
        )
    }

    @Test
    fun `비기본 앨범 요청의 dataVersion 계산에는 albumId를 유지한다`() {
        val albumId = 22L
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(albumRepository.findById(albumId)).thenReturn(createAlbum(id = albumId, isDefault = false))
        `when`(mapPhotosCacheService.getDataVersion(any(), any(), eq(1L), eq(albumId))).thenReturn(10L)
        `when`(mapPhotosCacheService.getClusteredPhotos(any(), any(), eq(1L), eq(albumId), any())).thenReturn(
            MapPhotosResponse(clusters = emptyList()),
        )
        `when`(mapClientPort.reverseGeocode(any(), any())).thenReturn(
            LocationInfoResponse(address = "서울 강남구", placeName = "역삼역", regionName = "강남구"),
        )
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(emptyList())
        `when`(albumRepository.photoCountSumByUserId(1L)).thenReturn(0)

        mapService.getMe(
            userId = 1L,
            longitude = 127.0,
            latitude = 37.5,
            zoom = 14.0,
            albumId = albumId,
            lastDataVersion = null,
        )

        verify(mapPhotosCacheService).getDataVersion(any(), any(), eq(1L), eq(albumId))
        verify(mapPhotosCacheService).getClusteredPhotos(any(), any(), eq(1L), eq(albumId), any())
    }

    @Test
    fun `getClusterPhotos는 연결되지 않은 유저면 빈 리스트를 반환한다`() {
        `when`(coupleRepository.findByUserId(1L)).thenReturn(null)

        val result = mapService.getClusterPhotos("z14_24661_7867", 1L)

        assertTrue(result.isEmpty())
        verify(mapQueryPort, org.mockito.Mockito.never()).findPhotosInGridCell(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `getClusterPhotos는 비로그인 요청에서 coupleId null로 조회한다`() {
        `when`(
            mapQueryPort.findPhotosInGridCell(
                any(),
                any(),
                any(),
                any(),
                anyOrNull(),
            ),
        ).thenReturn(emptyList())

        val result = mapService.getClusterPhotos("z14_24661_7867", null)

        assertTrue(result.isEmpty())
        verify(mapQueryPort).findPhotosInGridCell(any(), any(), any(), any(), anyOrNull())
    }
}
