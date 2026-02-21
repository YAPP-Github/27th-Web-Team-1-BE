package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelBasedClusterBoundaryMergeStrategyTest {
    private val strategy = PixelBasedClusterBoundaryMergeStrategy()

    @Test
    fun `3개는 가깝고 1개는 90px 멀면 3개만 병합된다`() {
        val zoom = 13.4
        val base = lonLatToWorldPx(127.0, 37.3, zoom)

        val clusters =
            listOf(
                cluster("z13_100_100", worldPxToLonLat(base.first, base.second, zoom)),
                cluster("z13_101_100", worldPxToLonLat(base.first + 20.0, base.second + 20.0, zoom)),
                cluster("z13_102_100", worldPxToLonLat(base.first + 30.0, base.second - 15.0, zoom)),
                cluster("z13_103_100", worldPxToLonLat(base.first + 1.0, base.second + 90.0, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)
        val counts = result.map { it.count }.sorted()

        assertEquals(listOf(1, 3), counts)
    }

    @Test
    fun `x 임계값을 초과하면 병합되지 않는다`() {
        val zoom = 12.8
        val base = lonLatToWorldPx(127.0, 37.3, zoom)
        val clusters =
            listOf(
                cluster("z12_1_1", worldPxToLonLat(base.first, base.second, zoom)),
                cluster("z12_2_1", worldPxToLonLat(base.first + 47.4, base.second + 10.0, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertEquals(2, result.size)
    }

    @Test
    fun `단일 마커는 complete-linkage 조건을 만족하지 않으면 자동 흡수되지 않는다`() {
        val zoom = 13.2
        val base = lonLatToWorldPx(127.0, 37.3, zoom)
        val clusters =
            listOf(
                cluster("z13_1_1", worldPxToLonLat(base.first + 0.0, base.second + 0.0, zoom)),
                cluster("z13_2_1", worldPxToLonLat(base.first + 49.0, base.second + 0.0, zoom)),
                cluster("z13_3_1", worldPxToLonLat(base.first + 49.0, base.second + 60.0, zoom)),
                cluster("z13_4_1", worldPxToLonLat(base.first + 51.5, base.second + 20.0, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertTrue(result.size >= 2)
        assertTrue(result.any { it.count >= 2 })
        assertTrue(result.any { it.count == 1 })
    }

    @Test
    fun `동일 clusterId가 여러 그룹에서 생성되면 suffix를 붙여 고유 ID를 보장한다`() {
        val zoom = 14.0
        val base = lonLatToWorldPx(127.1, 37.36, zoom)
        val farEnough = worldPxToLonLat(base.first + 55.0, base.second, zoom)
        val origin = worldPxToLonLat(base.first, base.second, zoom)
        val clusters =
            listOf(
                ClusterReadModel("z14_24681_7832", 1, "a.jpg", origin.first, origin.second),
                ClusterReadModel("z14_24681_7832", 2, "b.jpg", farEnough.first, farEnough.second),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertEquals(2, result.size)
        assertTrue(result.map { it.clusterId }.toSet().size == 2)
        assertTrue(result.any { it.clusterId == "z14_24681_7832" })
        assertTrue(result.any { it.clusterId == "z14_24681_7832_g2" })
    }

    @Test
    fun `매우 작은 줌 변화에서는 병합 결과가 안정적이다`() {
        val baseZoom = 14.356
        val nearZoom = 14.436
        val base = lonLatToWorldPx(127.0, 37.3, baseZoom)
        val clusters =
            listOf(
                cluster("z14_1_1", worldPxToLonLat(base.first, base.second, baseZoom)),
                cluster("z14_2_1", worldPxToLonLat(base.first + 38.0, base.second + 18.0, baseZoom)),
                cluster("z14_3_1", worldPxToLonLat(base.first + 45.0, base.second + 22.0, baseZoom)),
                cluster("z14_4_1", worldPxToLonLat(base.first + 5.0, base.second + 88.0, baseZoom)),
            )

        val a = strategy.mergeClusters(clusters, baseZoom).map { it.count }.sorted()
        val b = strategy.mergeClusters(clusters, nearZoom).map { it.count }.sorted()

        assertEquals(a, b)
    }

    @Test
    fun `json 케이스 - 1,2,1 에서 좌우 2와1은 zoom 14_032868718412672에서 병합된다`() {
        val zoom = 14.032868718412672
        val clusters =
            listOf(
                ClusterReadModel(
                    clusterId = "z14_24681_7833",
                    count = 1,
                    thumbnailUrl = "top.jpg",
                    longitude = 127.10613880132,
                    latitude = 37.370150892237,
                ),
                ClusterReadModel(
                    clusterId = "z14_24681_7832",
                    count = 2,
                    thumbnailUrl = "left.jpg",
                    longitude = 127.10548686836512,
                    latitude = 37.366947878141566,
                ),
                ClusterReadModel(
                    clusterId = "z14_24681_7832_g2",
                    count = 1,
                    thumbnailUrl = "right.jpg",
                    longitude = 127.108097457244,
                    latitude = 37.3661737923199,
                ),
            )

        val result = strategy.mergeClusters(clusters, zoom)
        assertEquals(listOf(1, 3), result.map { it.count }.sorted())
    }

    @Test
    fun `json 케이스 - 상하 1과3은 zoom 13_82475583775964에서 병합된다`() {
        val zoom = 13.82475583775964
        val clusters =
            listOf(
                ClusterReadModel(
                    clusterId = "z14_24681_7833",
                    count = 1,
                    thumbnailUrl = "top.jpg",
                    longitude = 127.10613880132,
                    latitude = 37.370150892237,
                ),
                ClusterReadModel(
                    clusterId = "z14_24681_7832",
                    count = 3,
                    thumbnailUrl = "bottom.jpg",
                    longitude = 127.10635706465808,
                    latitude = 37.366689849534346,
                ),
                ClusterReadModel(
                    clusterId = "z14_24682_7828",
                    count = 1,
                    thumbnailUrl = "far.jpg",
                    longitude = 127.10900662549734,
                    latitude = 37.35007159880101,
                ),
            )

        val result = strategy.mergeClusters(clusters, zoom)
        assertEquals(listOf(1, 4), result.map { it.count }.sorted())
    }

    @Test
    fun `동일 셀에서 분리된 g2 클러스터는 해당 그룹 멤버만 반환한다`() {
        val zoom = 14.0
        val base = lonLatToWorldPx(127.1, 37.36, zoom)
        val origin = worldPxToLonLat(base.first, base.second, zoom)
        val farEnough = worldPxToLonLat(base.first + 55.0, base.second, zoom)
        val cell = CellCoord(24681, 7832)
        val photos =
            listOf(
                ClusterPhotoMember(id = 1L, cell = cell, point = GeoPoint(origin.first, origin.second)),
                ClusterPhotoMember(id = 2L, cell = cell, point = GeoPoint(farEnough.first, farEnough.second)),
            )

        val result = strategy.resolveClusterPhotoIds(zoom = 14.0, photos = photos, targetClusterId = "z14_24681_7832_g2")

        assertEquals(setOf(2L), result)
    }

    @Test
    fun `동일 셀에서 기본 클러스터는 첫 번째 그룹 멤버를 반환한다`() {
        val zoom = 14.0
        val base = lonLatToWorldPx(127.1, 37.36, zoom)
        val origin = worldPxToLonLat(base.first, base.second, zoom)
        val farEnough = worldPxToLonLat(base.first + 55.0, base.second, zoom)
        val cell = CellCoord(24681, 7832)
        val photos =
            listOf(
                ClusterPhotoMember(id = 1L, cell = cell, point = GeoPoint(origin.first, origin.second)),
                ClusterPhotoMember(id = 2L, cell = cell, point = GeoPoint(farEnough.first, farEnough.second)),
            )

        val result = strategy.resolveClusterPhotoIds(zoom = 14.0, photos = photos, targetClusterId = "z14_24681_7832")

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `merge zoom 토큰이 있는 clusterId도 동일 그룹으로 매칭된다`() {
        val zoom = 14.0
        val base = lonLatToWorldPx(127.1, 37.36, zoom)
        val origin = worldPxToLonLat(base.first, base.second, zoom)
        val farEnough = worldPxToLonLat(base.first + 55.0, base.second, zoom)
        val cell = CellCoord(24681, 7832)
        val photos =
            listOf(
                ClusterPhotoMember(id = 1L, cell = cell, point = GeoPoint(origin.first, origin.second)),
                ClusterPhotoMember(id = 2L, cell = cell, point = GeoPoint(farEnough.first, farEnough.second)),
            )

        val result = strategy.resolveClusterPhotoIds(zoom = 14.0, photos = photos, targetClusterId = "z14_24681_7832_mz14000")

        assertEquals(setOf(1L), result)
    }

    @Test
    fun `그룹 매칭 실패 시 target cell 사진만 fallback 반환한다`() {
        val cell = CellCoord(24681, 7832)
        val neighbor = CellCoord(24681, 7833)
        val photos =
            listOf(
                ClusterPhotoMember(id = 1L, cell = cell, point = GeoPoint(127.10, 37.36)),
                ClusterPhotoMember(id = 2L, cell = neighbor, point = GeoPoint(127.10, 37.37)),
            )

        val result = strategy.resolveClusterPhotoIds(zoom = 14.0, photos = photos, targetClusterId = "z14_24681_7832_g9_mz14000")

        assertEquals(setOf(1L), result)
    }

    private fun cluster(
        clusterId: String,
        lonLat: Pair<Double, Double>,
    ): ClusterReadModel =
        ClusterReadModel(
            clusterId = clusterId,
            count = 1,
            thumbnailUrl = "$clusterId.jpg",
            longitude = lonLat.first,
            latitude = lonLat.second,
        )

    private fun lonLatToWorldPx(
        lon: Double,
        lat: Double,
        zoom: Double,
    ): Pair<Double, Double> {
        val worldSize = 256.0 * Math.pow(2.0, zoom)
        val x = (lon + 180.0) / 360.0 * worldSize
        val siny = kotlin.math.sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        val y = (0.5 - kotlin.math.ln((1 + siny) / (1 - siny)) / (4 * PI)) * worldSize
        return x to y
    }

    private fun worldPxToLonLat(
        x: Double,
        y: Double,
        zoom: Double,
    ): Pair<Double, Double> {
        val worldSize = 256.0 * Math.pow(2.0, zoom)
        val lon = (x / worldSize) * 360.0 - 180.0
        val n = PI - (2.0 * PI * y) / worldSize
        val lat = Math.toDegrees(atan(sinh(n)))
        return lon to lat
    }
}
