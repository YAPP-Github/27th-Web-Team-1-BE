package kr.co.lokit.api.domain.map.application

import com.github.benmanes.caffeine.cache.Caffeine
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.domain.BBox
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.anyDouble
import org.mockito.Mockito.eq
import org.mockito.Mockito.isNull
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class MapPhotosCacheServiceTest {
    @Mock
    lateinit var mapQueryPort: MapQueryPort

    @Mock
    lateinit var cacheManager: CacheManager

    lateinit var service: MapPhotosCacheService

    @BeforeEach
    fun setUp() {
        service = MapPhotosCacheService(mapQueryPort, cacheManager)
    }

    // --- buildCellKey 테스트 ---

    @Test
    fun `buildCellKey는 올바른 형식의 키를 생성한다`() {
        val key = service.buildCellKey(14, 100, 200, 1L, 2L, 3L)

        assertEquals("z14_x100_y200_c1_a2_v3", key)
    }

    @Test
    fun `buildCellKey에서 coupleId가 null이면 0으로 대체된다`() {
        val key = service.buildCellKey(14, 100, 200, null, 2L, 0L)

        assertEquals("z14_x100_y200_c0_a2_v0", key)
    }

    @Test
    fun `buildCellKey에서 albumId가 null이면 0으로 대체된다`() {
        val key = service.buildCellKey(14, 100, 200, 1L, null, 0L)

        assertEquals("z14_x100_y200_c1_a0_v0", key)
    }

    @Test
    fun `buildCellKey에서 coupleId와 albumId 모두 null이면 둘 다 0으로 대체된다`() {
        val key = service.buildCellKey(14, 100, 200, null, null, 0L)

        assertEquals("z14_x100_y200_c0_a0_v0", key)
    }

    @Test
    fun `buildCellKey에서 음수 셀 좌표도 올바르게 처리한다`() {
        val key = service.buildCellKey(10, -5, -10, 1L, null, 1L)

        assertEquals("z10_x-5_y-10_c1_a0_v1", key)
    }

    // --- buildIndividualKey (top-level function) 테스트 ---

    @Test
    fun `buildIndividualKey는 올바른 형식의 키를 생성한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key = buildIndividualKey(bbox, 14, 1L, 2L, 0L)

        assertTrue(key.startsWith("ind_z14_x"))
        assertTrue(key.contains("_c1_"))
        assertTrue(key.endsWith("_v0"))
    }

    @Test
    fun `buildIndividualKey에서 coupleId가 null이면 0으로 대체된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key = buildIndividualKey(bbox, 14, null, null, 0L)

        assertTrue(key.contains("_c0_"))
    }

    @Test
    fun `같은 파라미터로 buildIndividualKey를 호출하면 동일한 키가 생성된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key1 = buildIndividualKey(bbox, 14, 1L, null, 0L)
        val key2 = buildIndividualKey(bbox, 14, 1L, null, 0L)

        assertEquals(key1, key2)
    }

    @Test
    fun `다른 버전으로 buildIndividualKey를 호출하면 다른 키가 생성된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key1 = buildIndividualKey(bbox, 14, 1L, null, 0L)
        val key2 = buildIndividualKey(bbox, 14, 1L, null, 1L)

        assertTrue(key1 != key2)
    }

    // --- evictForCouple / getVersion 테스트 ---

    @Test
    fun `evictForCouple 호출 후 버전이 증가한다`() {
        val coupleId = 999L
        val versionBefore = service.getVersion(coupleId)

        service.evictForCouple(coupleId)

        val versionAfter = service.getVersion(coupleId)
        assertEquals(versionBefore + 1, versionAfter)
    }

    @Test
    fun `evictForCouple을 여러 번 호출하면 버전이 누적 증가한다`() {
        val coupleId = 998L
        service.evictForCouple(coupleId)
        service.evictForCouple(coupleId)
        service.evictForCouple(coupleId)

        val version = service.getVersion(coupleId)
        assertTrue(version >= 3)
    }

    @Test
    fun `getVersion에 null coupleId를 전달하면 0을 반환한다`() {
        val version = service.getVersion(null)

        assertEquals(0L, version)
    }

    @Test
    fun `존재하지 않는 coupleId의 버전은 0이다`() {
        val version = service.getVersion(Long.MAX_VALUE)

        assertEquals(0L, version)
    }

    // --- getClusteredPhotos 테스트 ---

    @Test
    fun `캐시가 없으면 queryDirectly로 폴백한다`() {
        `when`(cacheManager.getCache("mapCells")).thenReturn(null)
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)

        `when`(
            mapQueryPort.findClustersWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                gridSize = anyDouble(),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(emptyList())

        val result = service.getClusteredPhotos(14, bbox, null, null)

        assertNotNull(result)
        assertTrue(result.clusters.isNullOrEmpty())
    }

    @Test
    fun `캐시가 있고 모든 셀이 캐시에 있으면 DB 쿼리를 하지 않는다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)

        val bbox = BBox(126.9999, 37.4999, 127.0001, 37.5001)

        // 캐시에 셀 데이터를 미리 넣어둔다
        val zoom = 14
        val tempService = MapPhotosCacheService(mapQueryPort, cacheManager)
        val gridSize =
            kr.co.lokit.api.domain.map.domain.GridValues
                .getGridSize(zoom)

        val cellXMin = kotlin.math.floor(lonToMHelper(bbox.west) / gridSize).toLong()
        val cellXMax = kotlin.math.floor(lonToMHelper(bbox.east) / gridSize).toLong()
        val cellYMin = kotlin.math.floor(latToMHelper(bbox.south) / gridSize).toLong()
        val cellYMax = kotlin.math.floor(latToMHelper(bbox.north) / gridSize).toLong()

        for (cx in cellXMin..cellXMax) {
            for (cy in cellYMin..cellYMax) {
                val key = tempService.buildCellKey(zoom, cx, cy, null, null, 0L)
                caffeineCache.nativeCache.put(key, MapPhotosCacheService.CachedCell(null))
            }
        }

        val result = tempService.getClusteredPhotos(zoom, bbox, null, null)

        assertNotNull(result)
        verify(mapQueryPort, never()).findClustersWithinBBox(
            west = anyDouble(),
            south = anyDouble(),
            east = anyDouble(),
            north = anyDouble(),
            gridSize = anyDouble(),
            coupleId = any(),
            albumId = any(),
        )
    }

    @Test
    fun `캐시 미스인 셀은 DB에서 조회 후 캐시에 저장한다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)

        val bbox = BBox(126.9999, 37.4999, 127.0001, 37.5001)
        val now = LocalDateTime.now()

        val dbResult =
            listOf(
                ClusterProjection(
                    cellX = 0,
                    cellY = 0,
                    count = 5,
                    thumbnailUrl = "https://example.com/photo.jpg",
                    centerLongitude = 127.0,
                    centerLatitude = 37.5,
                    takenAt = now,
                ),
            )

        `when`(
            mapQueryPort.findClustersWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                gridSize = anyDouble(),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(dbResult)

        val result = service.getClusteredPhotos(14, bbox, null, null)

        assertNotNull(result)
    }

    // --- getIndividualPhotos 테스트 ---

    @Test
    fun `getIndividualPhotos는 mapQueryPort에서 사진을 조회하여 반환한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val now = LocalDateTime.now()
        val photos =
            listOf(
                PhotoProjection(
                    id = 1L,
                    url = "https://example.com/1.jpg",
                    longitude = 127.0,
                    latitude = 37.5,
                    takenAt = now,
                ),
                PhotoProjection(
                    id = 2L,
                    url = "https://example.com/2.jpg",
                    longitude = 127.01,
                    latitude = 37.51,
                    takenAt = now.minusDays(1),
                ),
            )

        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(photos)

        val result = service.getIndividualPhotos(17, bbox, null, null)

        assertNotNull(result.photos)
        assertEquals(2, result.photos!!.size)
        assertEquals(1L, result.photos!![0].id)
        assertEquals(2L, result.photos!![1].id)
    }

    @Test
    fun `getIndividualPhotos에서 사진이 없으면 빈 리스트를 반환한다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)

        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(emptyList())

        val result = service.getIndividualPhotos(17, bbox, null, null)

        assertNotNull(result.photos)
        assertTrue(result.photos!!.isEmpty())
        assertNull(result.clusters)
    }

    @Test
    fun `getIndividualPhotos에서 coupleId와 albumId를 전달할 수 있다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)

        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = eq(1L),
                albumId = eq(2L),
            ),
        ).thenReturn(emptyList())

        val result = service.getIndividualPhotos(17, bbox, 1L, 2L)

        assertNotNull(result.photos)
    }

    companion object {
        private const val EARTH_RADIUS = 6378137.0

        fun lonToMHelper(lon: Double): Double = lon * (Math.PI * EARTH_RADIUS / 180.0)

        fun latToMHelper(lat: Double): Double = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) * EARTH_RADIUS
    }
}
