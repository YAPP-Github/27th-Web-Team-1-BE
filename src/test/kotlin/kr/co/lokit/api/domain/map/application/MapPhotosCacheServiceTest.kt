package kr.co.lokit.api.domain.map.application

import com.github.benmanes.caffeine.cache.Caffeine
import kr.co.lokit.api.config.cache.CacheNames
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.ClusterId
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.MercatorProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.eq
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import java.time.LocalDateTime
import kotlin.math.floor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class MapPhotosCacheServiceTest {
    @org.mockito.Mock
    lateinit var mapQueryPort: MapQueryPort

    @org.mockito.Mock
    lateinit var cacheManager: CacheManager

    lateinit var service: MapPhotosCacheService

    @BeforeEach
    fun setUp() {
        service = MapPhotosCacheService(mapQueryPort, cacheManager, PixelBasedClusterBoundaryMergeStrategy())
    }

    @Test
    fun `buildCellKey formats values consistently`() {
        val key = service.buildCellKey(14, 100, 200, 1L, 2L, 3L)
        assertEquals("z14_x100_y200_c1_a2_v3", key)
    }

    @Test
    fun `buildCellKey normalizes nullable ids to zero`() {
        val key = service.buildCellKey(14, -5, -10, null, null, 0L)
        assertEquals("z14_x-5_y-10_c0_a0_v0", key)
    }

    @Test
    fun `evictForCouple increases version`() {
        val coupleId = 999L
        val before = service.getVersion(coupleId)

        service.evictForCouple(coupleId)

        assertEquals(before + 1, service.getVersion(coupleId))
    }

    @Test
    fun `getVersion returns zero for null couple`() {
        assertEquals(0L, service.getVersion(null))
    }

    @Test
    fun `getDataVersion is stable across bbox for same couple and album`() {
        service.evictForPhotoMutation(coupleId = 1L, albumId = 10L, longitude = 127.0, latitude = 37.5)
        val seoul = BBox(126.9, 37.4, 127.1, 37.6)
        val busan = BBox(129.0, 35.0, 129.2, 35.2)

        val seoulVersion = service.getDataVersion(14.0, seoul, 1L, 10L)
        val busanVersion = service.getDataVersion(14.0, busan, 1L, 10L)

        assertTrue(seoulVersion > 0L)
        assertEquals(seoulVersion, busanVersion)
    }

    @Test
    fun `getDataVersion can differ by album`() {
        service.evictForPhotoMutation(coupleId = 1L, albumId = 10L, longitude = 127.0, latitude = 37.5)
        service.evictForPhotoMutation(coupleId = 1L, albumId = 11L, longitude = 127.0, latitude = 37.5)
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)

        val v10 = service.getDataVersion(14.0, bbox, 1L, 10L)
        val v11 = service.getDataVersion(14.0, bbox, 1L, 11L)

        assertTrue(v10 > 0L)
        assertTrue(v11 > 0L)
        assertTrue(v10 != v11)
    }

    @Test
    fun `evictForCouple invalidates only matching map photos cache keys`() {
        val mapPhotos = CaffeineCache(CacheNames.MAP_PHOTOS, Caffeine.newBuilder().build())
        `when`(cacheManager.getCache(CacheNames.MAP_PHOTOS)).thenReturn(mapPhotos)

        val targetKey = MapCacheKeyFactory.buildIndividualKey(BBox(126.9, 37.4, 127.1, 37.6), 17.0, 1L, null, 0L)
        val otherKey = MapCacheKeyFactory.buildIndividualKey(BBox(126.9, 37.4, 127.1, 37.6), 17.0, 2L, null, 0L)
        mapPhotos.nativeCache.put(targetKey, "target")
        mapPhotos.nativeCache.put(otherKey, "other")

        service.evictForCouple(1L)

        assertNull(mapPhotos.nativeCache.getIfPresent(targetKey))
        assertNotNull(mapPhotos.nativeCache.getIfPresent(otherKey))
    }

    @Test
    fun `evictForPhotoMutation invalidates only matching point keys`() {
        val mapPhotos = CaffeineCache(CacheNames.MAP_PHOTOS, Caffeine.newBuilder().build())
        `when`(cacheManager.getCache(CacheNames.MAP_PHOTOS)).thenReturn(mapPhotos)

        val target = MapCacheKeyFactory.buildIndividualKey(BBox(126.9, 37.4, 127.1, 37.6), 17.0, 1L, null, 0L)
        val otherCouple = MapCacheKeyFactory.buildIndividualKey(BBox(126.9, 37.4, 127.1, 37.6), 17.0, 2L, null, 0L)
        val outsidePoint = MapCacheKeyFactory.buildIndividualKey(BBox(128.0, 38.0, 128.2, 38.2), 17.0, 1L, null, 0L)
        mapPhotos.nativeCache.put(target, "target")
        mapPhotos.nativeCache.put(otherCouple, "other-couple")
        mapPhotos.nativeCache.put(outsidePoint, "outside")

        service.evictForPhotoMutation(coupleId = 1L, albumId = null, longitude = 127.0, latitude = 37.5)

        assertNull(mapPhotos.nativeCache.getIfPresent(target))
        assertNotNull(mapPhotos.nativeCache.getIfPresent(otherCouple))
        assertNotNull(mapPhotos.nativeCache.getIfPresent(outsidePoint))
    }

    @Test
    fun `getClusteredPhotos queries raw photos and returns mapped clusters`() {
        val bbox = BBox(126.99, 37.49, 127.01, 37.51)
        val now = LocalDateTime.now()
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = eq(1L),
                albumId = eq(2L),
            ),
        ).thenReturn(
            listOf(
                PhotoProjection(id = 1L, url = "a.jpg", longitude = 127.0, latitude = 37.5, takenAt = now),
                PhotoProjection(id = 2L, url = "b.jpg", longitude = 127.0001, latitude = 37.5001, takenAt = now.minusDays(1)),
            ),
        )

        val result = service.getClusteredPhotos(14.0, bbox, 1L, 2L)

        assertNotNull(result.clusters)
        assertEquals(2, result.clusters!!.asList().sumOf { it.count })
        verify(mapQueryPort).findPhotosWithinBBox(
            west = eq(bbox.west),
            south = eq(bbox.south),
            east = eq(bbox.east),
            north = eq(bbox.north),
            coupleId = eq(1L),
            albumId = eq(2L),
        )
    }

    @Test
    fun `getClusteredPhotos uses floored zoom grid for cluster id`() {
        val bbox = BBox(126.99, 37.49, 127.01, 37.51)
        val zoom = 14.7
        val longitude = 127.0
        val latitude = 37.5
        val now = LocalDateTime.now()
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = eq(1L),
                albumId = isNull(),
            ),
        ).thenReturn(
            listOf(
                PhotoProjection(
                    id = 1L,
                    url = "a.jpg",
                    longitude = longitude,
                    latitude = latitude,
                    takenAt = now,
                ),
            ),
        )

        val result = service.getClusteredPhotos(zoom, bbox, 1L, null)
        val clusterId = result.clusters!!.asList().single().clusterId
        val parsedClusterId = ClusterId.parseDetailed(clusterId)

        val discreteZoom = floor(zoom).toInt()
        val gridSize = GridValues.getGridSize(discreteZoom)
        val expectedX = floor(MercatorProjection.longitudeToMeters(longitude) / gridSize).toLong()
        val expectedY = floor(MercatorProjection.latitudeToMeters(latitude) / gridSize).toLong()
        assertEquals(discreteZoom, parsedClusterId.zoom)
        assertEquals(expectedX, parsedClusterId.cellX)
        assertEquals(expectedY, parsedClusterId.cellY)
        assertEquals(zoom, parsedClusterId.mergeZoom)
    }

    @Test
    fun `getClusteredPhotos preserves raw clusters when merge drops some counts`() {
        val bbox = BBox(126.99, 37.49, 127.01, 37.51)
        val now = LocalDateTime.now()
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = eq(1L),
                albumId = isNull(),
            ),
        ).thenReturn(
            listOf(
                PhotoProjection(id = 1L, url = "a.jpg", longitude = 127.0, latitude = 37.5, takenAt = now),
                PhotoProjection(id = 2L, url = "b.jpg", longitude = 127.1, latitude = 37.6, takenAt = now.minusMinutes(1)),
            ),
        )

        val droppingStrategy =
            object : ClusterBoundaryMergeStrategy {
                override fun mergeClusters(
                    clusters: List<kr.co.lokit.api.domain.map.domain.ClusterReadModel>,
                    zoom: Double,
                ): List<kr.co.lokit.api.domain.map.domain.ClusterReadModel> = clusters.take(1)

                override fun resolveClusterCells(
                    zoom: Int,
                    photosByCell: Map<CellCoord, List<GeoPoint>>,
                    targetCell: CellCoord,
                ): Set<CellCoord> = setOf(targetCell)
            }
        val customService = MapPhotosCacheService(mapQueryPort, cacheManager, droppingStrategy)

        val result = customService.getClusteredPhotos(14.0, bbox, 1L, null)

        assertNotNull(result.clusters)
        assertEquals(2, result.clusters!!.asList().sumOf { it.count })
    }

    @Test
    fun `getClusteredPhotos triggers prefetch when viewport moves with same couple`() {
        val first = BBox(126.95, 37.45, 127.00, 37.50)
        val second = BBox(126.99, 37.45, 127.04, 37.50)
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                coupleId = eq(1L),
                albumId = isNull(),
            ),
        ).thenReturn(emptyList())

        service.getClusteredPhotos(14.0, first, 1L, null)
        service.getClusteredPhotos(14.0, second, 1L, null)

        verify(mapQueryPort, timeout(1000).atLeast(3)).findPhotosWithinBBox(
            west = anyDouble(),
            south = anyDouble(),
            east = anyDouble(),
            north = anyDouble(),
            coupleId = eq(1L),
            albumId = isNull(),
        )
    }

    @Test
    fun `getClusteredPhotos does not prefetch for anonymous couple`() {
        val first = BBox(126.95, 37.45, 127.00, 37.50)
        val second = BBox(126.99, 37.45, 127.04, 37.50)
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = anyDouble(),
                south = anyDouble(),
                east = anyDouble(),
                north = anyDouble(),
                coupleId = isNull(),
                albumId = isNull(),
            ),
        ).thenReturn(emptyList())

        service.getClusteredPhotos(14.0, first, null, null)
        service.getClusteredPhotos(14.0, second, null, null)

        verify(mapQueryPort, times(2)).findPhotosWithinBBox(
            west = anyDouble(),
            south = anyDouble(),
            east = anyDouble(),
            north = anyDouble(),
            coupleId = isNull(),
            albumId = isNull(),
        )
    }

    @Test
    fun `getIndividualPhotos returns mapped photos`() {
        val bbox = BBox(126.9, 37.4, 127.1, 37.6)
        val now = LocalDateTime.now()
        `when`(
            mapQueryPort.findPhotosWithinBBox(
                west = eq(bbox.west),
                south = eq(bbox.south),
                east = eq(bbox.east),
                north = eq(bbox.north),
                coupleId = eq(1L),
                albumId = eq(2L),
            ),
        ).thenReturn(
            listOf(
                PhotoProjection(id = 1L, url = "1.jpg", longitude = 127.0, latitude = 37.5, takenAt = now),
                PhotoProjection(id = 2L, url = "2.jpg", longitude = 127.01, latitude = 37.51, takenAt = now.minusDays(1)),
            ),
        )

        val result = service.getIndividualPhotos(17.0, bbox, 1L, 2L)

        assertNotNull(result.photos)
        assertEquals(2, result.photos!!.asList().size)
        assertNull(result.clusters)
    }
}
