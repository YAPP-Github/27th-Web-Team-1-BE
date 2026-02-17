package kr.co.lokit.api.domain.map.application

import com.github.benmanes.caffeine.cache.Caffeine
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import kr.co.lokit.api.domain.map.dto.ClusterResponse
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
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
import org.mockito.ArgumentCaptor
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
        service = MapPhotosCacheService(mapQueryPort, cacheManager, LegacyClusterBoundaryMergeStrategy())
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
        val key = MapCacheKeyFactory.buildIndividualKey(bbox, 14, 1L, 2L, 0L)

        assertTrue(key.startsWith("ind_z14_w"))
        assertTrue(key.contains("_c1_"))
        assertTrue(key.contains("_a2_"))
        assertTrue(key.endsWith("_v0"))
    }

    @Test
    fun `buildIndividualKey에서 coupleId가 null이면 0으로 대체된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key = MapCacheKeyFactory.buildIndividualKey(bbox, 14, null, null, 0L)

        assertTrue(key.contains("_c0_"))
    }

    @Test
    fun `같은 파라미터로 buildIndividualKey를 호출하면 동일한 키가 생성된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key1 = MapCacheKeyFactory.buildIndividualKey(bbox, 14, 1L, null, 0L)
        val key2 = MapCacheKeyFactory.buildIndividualKey(bbox, 14, 1L, null, 0L)

        assertEquals(key1, key2)
    }

    @Test
    fun `다른 버전으로 buildIndividualKey를 호출하면 다른 키가 생성된다`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val key1 = MapCacheKeyFactory.buildIndividualKey(bbox, 14, 1L, null, 0L)
        val key2 = MapCacheKeyFactory.buildIndividualKey(bbox, 14, 1L, null, 1L)

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

    @Test
    fun `evictForCouple은 해당 커플의 map 캐시 키만 제거한다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val targetCellKey = service.buildCellKey(14, 10, 20, 1L, null, 0L)
        val targetPhotoKey = MapCacheKeyFactory.buildIndividualKey(bbox, 17, 1L, null, 0L)
        val otherCellKey = service.buildCellKey(14, 10, 20, 2L, null, 0L)

        mapCells.nativeCache.put(targetCellKey, MapPhotosCacheService.CachedCell(null))
        mapPhotos.nativeCache.put(targetPhotoKey, MapPhotosResponse(photos = emptyList()))
        mapCells.nativeCache.put(otherCellKey, MapPhotosCacheService.CachedCell(null))

        service.evictForCouple(1L)

        assertNull(mapCells.nativeCache.getIfPresent(targetCellKey))
        assertNull(mapPhotos.nativeCache.getIfPresent(targetPhotoKey))
        assertNotNull(mapCells.nativeCache.getIfPresent(otherCellKey))
    }

    @Test
    fun `위치 기반 version은 bbox 내부 변경만 반영한다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        service.evictForPhotoMutation(coupleId = 1L, albumId = 10L, longitude = 127.0, latitude = 37.5)

        val inside = BBox(126.9, 37.4, 127.1, 37.6)
        val outside = BBox(128.0, 38.0, 128.1, 38.1)

        assertTrue(service.getVersion(14, inside, 1L, null) > 0L)
        assertEquals(0L, service.getVersion(14, outside, 1L, null))
    }

    @Test
    fun `위치 기반 version은 album 필터를 구분한다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        val coupleId = 11_001L
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        service.evictForPhotoMutation(coupleId = coupleId, albumId = 10L, longitude = 127.0, latitude = 37.5)

        assertTrue(service.getVersion(14, bbox, coupleId, 10L) > 0L)
        assertEquals(0L, service.getVersion(14, bbox, coupleId, 11L))
    }

    @Test
    fun `dataVersion은 위치가 달라도 동일한 전역 버전을 반환한다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        service.evictForPhotoMutation(coupleId = 1L, albumId = 10L, longitude = 127.0, latitude = 37.5)

        val seoulBbox = BBox(126.9, 37.4, 127.1, 37.6)
        val busanBbox = BBox(129.0, 35.0, 129.2, 35.2)

        val seoulVersion = service.getDataVersion(14, seoulBbox, 1L, 10L)
        val busanVersion = service.getDataVersion(14, busanBbox, 1L, 10L)

        assertEquals(seoulVersion, busanVersion)
        assertTrue(seoulVersion > 0L)
    }

    @Test
    fun `dataVersion은 albumId가 다르면 서로 다른 값을 반환할 수 있다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        service.evictForPhotoMutation(coupleId = 1L, albumId = 10L, longitude = 127.0, latitude = 37.5)
        service.evictForPhotoMutation(coupleId = 1L, albumId = 11L, longitude = 127.0, latitude = 37.5)

        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val v10 = service.getDataVersion(14, bbox, 1L, 10L)
        val v11 = service.getDataVersion(14, bbox, 1L, 11L)

        assertTrue(v10 > 0L)
        assertTrue(v11 > 0L)
        assertTrue(v10 != v11)
    }

    @Test
    fun `evictForPhotoMutation은 동일 couple과 좌표에 해당하는 캐시만 무효화한다`() {
        val mapCells = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(mapCells)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        val zoom = 14
        val gridSize = GridValues.getGridSize(zoom)
        val cellX = kotlin.math.floor(lonToMHelper(127.0) / gridSize).toLong()
        val cellY = kotlin.math.floor(latToMHelper(37.5) / gridSize).toLong()
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val targetCellKey = service.buildCellKey(zoom, cellX, cellY, 1L, null, 0L)
        val otherCoupleCellKey = service.buildCellKey(zoom, cellX, cellY, 2L, null, 0L)
        val targetIndividualKey = MapCacheKeyFactory.buildIndividualKey(bbox, 17, 1L, null, 0L)
        val otherCoupleIndividualKey = MapCacheKeyFactory.buildIndividualKey(bbox, 17, 2L, null, 0L)

        mapCells.nativeCache.put(targetCellKey, MapPhotosCacheService.CachedCell(null))
        mapCells.nativeCache.put(otherCoupleCellKey, MapPhotosCacheService.CachedCell(null))
        mapPhotos.nativeCache.put(targetIndividualKey, MapPhotosResponse(photos = emptyList()))
        mapPhotos.nativeCache.put(otherCoupleIndividualKey, MapPhotosResponse(photos = emptyList()))

        service.evictForPhotoMutation(coupleId = 1L, albumId = null, longitude = 127.0, latitude = 37.5)

        assertNull(mapCells.nativeCache.getIfPresent(targetCellKey))
        assertNull(mapPhotos.nativeCache.getIfPresent(targetIndividualKey))
        assertNotNull(mapCells.nativeCache.getIfPresent(otherCoupleCellKey))
        assertNotNull(mapPhotos.nativeCache.getIfPresent(otherCoupleIndividualKey))
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
    fun `조회 셀이 너무 많으면 캐시를 사용하지 않고 queryDirectly로 폴백한다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)
        val zoom = 14
        val gridSize = GridValues.getGridSize(zoom)
        val westMeters = 10_000.0
        val southMeters = 10_000.0
        val eastMeters = westMeters + gridSize * 23.0
        val northMeters = southMeters + gridSize * 23.0
        val wideBbox =
            BBox(
                west = MercatorProjection.metersToLongitude(westMeters),
                south = MercatorProjection.metersToLatitude(southMeters),
                east = MercatorProjection.metersToLongitude(eastMeters),
                north = MercatorProjection.metersToLatitude(northMeters),
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
        ).thenReturn(emptyList())

        val result = service.getClusteredPhotos(zoom, wideBbox, null, null)

        assertNotNull(result)
        verify(mapQueryPort).findClustersWithinBBox(
            west = anyDouble(),
            south = anyDouble(),
            east = anyDouble(),
            north = anyDouble(),
            gridSize = anyDouble(),
            coupleId = isNull(),
            albumId = isNull(),
        )
    }

    @Test
    fun `캐시가 있고 모든 셀이 캐시에 있으면 DB 쿼리를 하지 않는다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)

        val bbox = BBox(126.9999, 37.4999, 127.0001, 37.5001)

        // 캐시에 셀 데이터를 미리 넣어둔다
        val zoom = 14
        val tempService = MapPhotosCacheService(mapQueryPort, cacheManager, LegacyClusterBoundaryMergeStrategy())
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

    @Test
    fun `일부 셀만 캐시되어 있으면 미캐시 셀만 DB 조회하고 hit와 miss를 합쳐 반환한다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)

        val zoom = 14
        val gridSize = GridValues.getGridSize(zoom)
        val baseX = 1000L
        val baseY = 2000L
        val cachedCellX = baseX
        val missedCellX = baseX + 1
        val cellY = baseY

        val westMeters = baseX * gridSize + 1.0
        val southMeters = baseY * gridSize + 1.0
        val eastMeters = (baseX + 2) * gridSize - 1.0
        val northMeters = (baseY + 1) * gridSize - 1.0
        val bbox =
            BBox(
                west = MercatorProjection.metersToLongitude(westMeters),
                south = MercatorProjection.metersToLatitude(southMeters),
                east = MercatorProjection.metersToLongitude(eastMeters),
                north = MercatorProjection.metersToLatitude(northMeters),
            )

        val expectedWest = missedCellX * gridSize
        val expectedSouth = cellY * gridSize
        val expectedEast = (missedCellX + 1) * gridSize
        val expectedNorth = (cellY + 1) * gridSize

        val version = service.getVersion(zoom, bbox, null, null)
        val cachedKey = service.buildCellKey(zoom, cachedCellX, cellY, null, null, version)
        val missedKey = service.buildCellKey(zoom, missedCellX, cellY, null, null, version)

        caffeineCache.nativeCache.put(
            cachedKey,
            MapPhotosCacheService.CachedCell(
                ClusterResponse(
                    clusterId = ClusterId.format(zoom, cachedCellX, cellY),
                    count = 1,
                    thumbnailUrl = "cached.jpg",
                    longitude = MercatorProjection.metersToLongitude(cachedCellX * gridSize + (gridSize / 2.0)),
                    latitude = MercatorProjection.metersToLatitude(cellY * gridSize + (gridSize / 2.0)),
                ),
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
        ).thenReturn(
            listOf(
                ClusterProjection(
                    cellX = missedCellX,
                    cellY = cellY,
                    count = 2,
                    thumbnailUrl = "missed.jpg",
                    centerLongitude = MercatorProjection.metersToLongitude(missedCellX * gridSize + (gridSize / 2.0)),
                    centerLatitude = MercatorProjection.metersToLatitude(cellY * gridSize + (gridSize / 2.0)),
                    takenAt = LocalDateTime.now(),
                ),
            ),
        )

        val result = service.getClusteredPhotos(zoom, bbox, null, null)

        assertNotNull(result.clusters)
        assertEquals(2, result.clusters!!.size)
        assertNotNull(caffeineCache.nativeCache.getIfPresent(missedKey))

        val westCaptor = ArgumentCaptor.forClass(Double::class.java)
        val southCaptor = ArgumentCaptor.forClass(Double::class.java)
        val eastCaptor = ArgumentCaptor.forClass(Double::class.java)
        val northCaptor = ArgumentCaptor.forClass(Double::class.java)
        val gridSizeCaptor = ArgumentCaptor.forClass(Double::class.java)
        verify(mapQueryPort).findClustersWithinBBox(
            westCaptor.capture(),
            southCaptor.capture(),
            eastCaptor.capture(),
            northCaptor.capture(),
            gridSizeCaptor.capture(),
            isNull(),
            isNull(),
        )

        assertEquals(expectedWest, westCaptor.value, 1e-6)
        assertEquals(expectedSouth, southCaptor.value, 1e-6)
        assertEquals(expectedEast, eastCaptor.value, 1e-6)
        assertEquals(expectedNorth, northCaptor.value, 1e-6)
        assertEquals(gridSize, gridSizeCaptor.value, 1e-9)
    }

    @Test
    fun `셀 캐시 재사용이 비활성화되면 캐시 hit가 있어도 요청 범위를 전체 재조회한다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)

        val zoom = 14
        val gridSize = GridValues.getGridSize(zoom)
        val baseX = 3000L
        val baseY = 4000L
        val cellY = baseY

        val westMeters = baseX * gridSize + 1.0
        val southMeters = baseY * gridSize + 1.0
        val eastMeters = (baseX + 2) * gridSize - 1.0
        val northMeters = (baseY + 1) * gridSize - 1.0
        val bbox =
            BBox(
                west = MercatorProjection.metersToLongitude(westMeters),
                south = MercatorProjection.metersToLatitude(southMeters),
                east = MercatorProjection.metersToLongitude(eastMeters),
                north = MercatorProjection.metersToLatitude(northMeters),
            )

        val version = service.getVersion(zoom, bbox, null, null)
        val cachedKey = service.buildCellKey(zoom, baseX, cellY, null, null, version)
        caffeineCache.nativeCache.put(
            cachedKey,
            MapPhotosCacheService.CachedCell(
                ClusterResponse(
                    clusterId = ClusterId.format(zoom, baseX, cellY),
                    count = 1,
                    thumbnailUrl = "cached.jpg",
                    longitude = MercatorProjection.metersToLongitude(baseX * gridSize + (gridSize / 2.0)),
                    latitude = MercatorProjection.metersToLatitude(cellY * gridSize + (gridSize / 2.0)),
                ),
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
        ).thenReturn(
            listOf(
                ClusterProjection(
                    cellX = baseX + 1,
                    cellY = cellY,
                    count = 1,
                    thumbnailUrl = "missed.jpg",
                    centerLongitude = MercatorProjection.metersToLongitude((baseX + 1) * gridSize + (gridSize / 2.0)),
                    centerLatitude = MercatorProjection.metersToLatitude(cellY * gridSize + (gridSize / 2.0)),
                    takenAt = LocalDateTime.now(),
                ),
            ),
        )

        service.getClusteredPhotos(zoom, bbox, null, null, zoom.toDouble(), canReuseCellCache = false)

        val westCaptor = ArgumentCaptor.forClass(Double::class.java)
        val southCaptor = ArgumentCaptor.forClass(Double::class.java)
        val eastCaptor = ArgumentCaptor.forClass(Double::class.java)
        val northCaptor = ArgumentCaptor.forClass(Double::class.java)
        verify(mapQueryPort).findClustersWithinBBox(
            westCaptor.capture(),
            southCaptor.capture(),
            eastCaptor.capture(),
            northCaptor.capture(),
            eq(gridSize),
            isNull(),
            isNull(),
        )

        assertEquals(baseX * gridSize, westCaptor.value, 1e-6)
        assertEquals(cellY * gridSize, southCaptor.value, 1e-6)
        assertEquals((baseX + 2) * gridSize, eastCaptor.value, 1e-6)
        assertEquals((cellY + 1) * gridSize, northCaptor.value, 1e-6)
    }

    @Test
    fun `버전 불일치여도 변경 없는 셀은 재사용하고 변경 셀만 DB 재조회한다`() {
        val caffeineCache = CaffeineCache("mapCells", Caffeine.newBuilder().build())
        val mapPhotos = CaffeineCache("mapPhotos", Caffeine.newBuilder().build())
        `when`(cacheManager.getCache("mapCells")).thenReturn(caffeineCache)
        `when`(cacheManager.getCache("mapPhotos")).thenReturn(mapPhotos)

        val zoom = 14
        val gridSize = GridValues.getGridSize(zoom)
        val baseX = 5000L
        val baseY = 6000L
        val firstCell = baseX to baseY
        val secondCell = (baseX + 1) to baseY
        val bbox =
            BBox(
                west = MercatorProjection.metersToLongitude(baseX * gridSize + 1.0),
                south = MercatorProjection.metersToLatitude(baseY * gridSize + 1.0),
                east = MercatorProjection.metersToLongitude((baseX + 2) * gridSize - 1.0),
                north = MercatorProjection.metersToLatitude((baseY + 1) * gridSize - 1.0),
            )
        val coupleId = 1L

        `when`(
            mapQueryPort.findClustersWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                gridSize = anyDouble(),
                coupleId = eq(coupleId),
                albumId = isNull(),
            ),
        ).thenReturn(
            listOf(
                ClusterProjection(
                    cellX = firstCell.first,
                    cellY = firstCell.second,
                    count = 1,
                    thumbnailUrl = "first.jpg",
                    centerLongitude = MercatorProjection.metersToLongitude(firstCell.first * gridSize + (gridSize / 2.0)),
                    centerLatitude = MercatorProjection.metersToLatitude(firstCell.second * gridSize + (gridSize / 2.0)),
                    takenAt = LocalDateTime.now(),
                ),
                ClusterProjection(
                    cellX = secondCell.first,
                    cellY = secondCell.second,
                    count = 1,
                    thumbnailUrl = "second.jpg",
                    centerLongitude = MercatorProjection.metersToLongitude(secondCell.first * gridSize + (gridSize / 2.0)),
                    centerLatitude = MercatorProjection.metersToLatitude(secondCell.second * gridSize + (gridSize / 2.0)),
                    takenAt = LocalDateTime.now(),
                ),
            ),
        )

        service.getClusteredPhotos(zoom, bbox, coupleId, null)

        org.mockito.Mockito.reset(mapQueryPort)
        `when`(
            mapQueryPort.findClustersWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                gridSize = anyDouble(),
                coupleId = eq(coupleId),
                albumId = isNull(),
            ),
        ).thenReturn(
            listOf(
                ClusterProjection(
                    cellX = secondCell.first,
                    cellY = secondCell.second,
                    count = 2,
                    thumbnailUrl = "second-new.jpg",
                    centerLongitude = MercatorProjection.metersToLongitude(secondCell.first * gridSize + (gridSize / 2.0)),
                    centerLatitude = MercatorProjection.metersToLatitude(secondCell.second * gridSize + (gridSize / 2.0)),
                    takenAt = LocalDateTime.now(),
                ),
            ),
        )

        service.evictForPhotoMutation(
            coupleId = coupleId,
            albumId = null,
            longitude = MercatorProjection.metersToLongitude(secondCell.first * gridSize + (gridSize / 2.0)),
            latitude = MercatorProjection.metersToLatitude(secondCell.second * gridSize + (gridSize / 2.0)),
        )

        service.getClusteredPhotos(zoom, bbox, coupleId, null, zoom.toDouble(), canReuseCellCache = false)

        val westCaptor = ArgumentCaptor.forClass(Double::class.java)
        val southCaptor = ArgumentCaptor.forClass(Double::class.java)
        val eastCaptor = ArgumentCaptor.forClass(Double::class.java)
        val northCaptor = ArgumentCaptor.forClass(Double::class.java)
        verify(mapQueryPort).findClustersWithinBBox(
            westCaptor.capture(),
            southCaptor.capture(),
            eastCaptor.capture(),
            northCaptor.capture(),
            eq(gridSize),
            eq(coupleId),
            isNull(),
        )

        assertEquals(secondCell.first * gridSize, westCaptor.value, 1e-6)
        assertEquals(secondCell.second * gridSize, southCaptor.value, 1e-6)
        assertEquals((secondCell.first + 1) * gridSize, eastCaptor.value, 1e-6)
        assertEquals((secondCell.second + 1) * gridSize, northCaptor.value, 1e-6)
    }

    @Test
    fun `인접 셀 경계에서 가까운 클러스터는 병합된다`() {
        `when`(cacheManager.getCache("mapCells")).thenReturn(null)
        val bbox = BBox(126.9, 37.3, 127.2, 37.4)
        val now = LocalDateTime.now()
        val dbResult =
            listOf(
                ClusterProjection(
                    cellX = 3085,
                    cellY = 978,
                    count = 1,
                    thumbnailUrl = "a.jpg",
                    centerLongitude = 127.112588277624,
                    centerLatitude = 37.3602093121085,
                    takenAt = now,
                ),
                ClusterProjection(
                    cellX = 3085,
                    cellY = 979,
                    count = 1,
                    thumbnailUrl = "b.jpg",
                    centerLongitude = 127.108097457244,
                    centerLatitude = 37.3661737923199,
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

        val result = MapPhotosCacheService(mapQueryPort, cacheManager, DistanceBasedClusterBoundaryMergeStrategy())
            .getClusteredPhotos(11, bbox, null, null)

        assertNotNull(result.clusters)
        assertEquals(1, result.clusters!!.size)
        assertEquals(2, result.clusters!![0].count)
    }

    @Test
    fun `인접 셀이라도 충분히 멀면 병합되지 않는다`() {
        `when`(cacheManager.getCache("mapCells")).thenReturn(null)
        val bbox = BBox(126.9, 37.3, 127.2, 37.4)
        val now = LocalDateTime.now()
        val dbResult =
            listOf(
                ClusterProjection(
                    cellX = 100,
                    cellY = 200,
                    count = 1,
                    thumbnailUrl = "a.jpg",
                    centerLongitude = 127.0,
                    centerLatitude = 37.0,
                    takenAt = now,
                ),
                ClusterProjection(
                    cellX = 100,
                    cellY = 201,
                    count = 1,
                    thumbnailUrl = "b.jpg",
                    centerLongitude = 127.05,
                    centerLatitude = 37.1,
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

        val result = MapPhotosCacheService(mapQueryPort, cacheManager, DistanceBasedClusterBoundaryMergeStrategy())
            .getClusteredPhotos(11, bbox, null, null)

        assertNotNull(result.clusters)
        assertEquals(2, result.clusters!!.size)
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

    @Test
    fun `server side raw clustering이 켜지면 원시 POI 조회를 사용한다`() {
        val rawService =
            MapPhotosCacheService(
                mapQueryPort = mapQueryPort,
                cacheManager = cacheManager,
                clusterBoundaryMergeStrategy = PixelBasedClusterBoundaryMergeStrategy(),
                serverSideRawClustering = true,
            )
        val zoomLevel = 13.4
        val discreteZoom = 13
        val bbox = BBox(126.95, 37.25, 127.05, 37.35)
        val now = LocalDateTime.now()

        val base = lonLatToWorldPx(127.0, 37.3, zoomLevel)
        val near = worldPxToLonLat(base.first + 20.0, base.second + 20.0, zoomLevel)

        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(
            listOf(
                PhotoProjection(
                    id = 1L,
                    url = "a.jpg",
                    longitude = 127.0,
                    latitude = 37.3,
                    takenAt = now,
                ),
                PhotoProjection(
                    id = 2L,
                    url = "b.jpg",
                    longitude = near.first,
                    latitude = near.second,
                    takenAt = now.plusMinutes(1),
                ),
            ),
        )

        val result = rawService.getClusteredPhotos(discreteZoom, bbox, null, null, zoomLevel)

        verify(mapQueryPort).findPhotosWithinBBox(
            west = eq(bbox.west),
            south = eq(bbox.south),
            east = eq(bbox.east),
            north = eq(bbox.north),
            coupleId = isNull(),
            albumId = isNull(),
        )
        verify(mapQueryPort, never()).findClustersWithinBBox(
            west = anyDouble(),
            south = anyDouble(),
            east = anyDouble(),
            north = anyDouble(),
            gridSize = anyDouble(),
            coupleId = any(),
            albumId = any(),
        )

        assertNotNull(result.clusters)
        assertEquals(1, result.clusters!!.size)
        assertEquals(2, result.clusters!!.first().count)
    }

    companion object {
        private const val EARTH_RADIUS = 6378137.0

        fun lonToMHelper(lon: Double): Double = lon * (Math.PI * EARTH_RADIUS / 180.0)

        fun latToMHelper(lat: Double): Double = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) * EARTH_RADIUS

        private fun lonLatToWorldPx(
            lon: Double,
            lat: Double,
            zoom: Double,
        ): Pair<Double, Double> {
            val worldSize = 256.0 * Math.pow(2.0, zoom)
            val x = (lon + 180.0) / 360.0 * worldSize
            val siny = kotlin.math.sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
            val y = (0.5 - kotlin.math.ln((1 + siny) / (1 - siny)) / (4 * Math.PI)) * worldSize
            return x to y
        }

        private fun worldPxToLonLat(
            x: Double,
            y: Double,
            zoom: Double,
        ): Pair<Double, Double> {
            val worldSize = 256.0 * Math.pow(2.0, zoom)
            val lon = (x / worldSize) * 360.0 - 180.0
            val n = Math.PI - (2.0 * Math.PI * y) / worldSize
            val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))
            return lon to lat
        }
    }
}
