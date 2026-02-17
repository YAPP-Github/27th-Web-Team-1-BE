package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.dto.ClusterResponse
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
    fun `체인 형태 A-B, B-C가 가능해도 A-C가 멀면 단일 클러스터로 합쳐지지 않는다`() {
        val zoom = 13.0
        val base = lonLatToWorldPx(127.0, 37.3, zoom)
        val clusters =
            listOf(
                cluster("z13_1_1", worldPxToLonLat(base.first, base.second, zoom)),
                cluster("z13_2_1", worldPxToLonLat(base.first + 40.0, base.second, zoom)),
                cluster("z13_3_1", worldPxToLonLat(base.first + 80.0, base.second, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)
        val counts = result.map { it.count }.sorted()

        assertEquals(listOf(1, 2), counts)
    }

    @Test
    fun `x y 임계값을 모두 만족하면 병합된다`() {
        val zoom = 12.8
        val base = lonLatToWorldPx(127.0, 37.3, zoom)
        val clusters =
            listOf(
                cluster("z12_1_1", worldPxToLonLat(base.first, base.second, zoom)),
                cluster("z12_2_1", worldPxToLonLat(base.first + 49.3333, base.second + 66.6666, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertEquals(1, result.size)
        assertEquals(2, result.first().count)
    }

    @Test
    fun `x 임계값을 초과하면 병합되지 않는다`() {
        val zoom = 12.8
        val base = lonLatToWorldPx(127.0, 37.3, zoom)
        val clusters =
            listOf(
                cluster("z12_1_1", worldPxToLonLat(base.first, base.second, zoom)),
                cluster("z12_2_1", worldPxToLonLat(base.first + 49.3334, base.second + 10.0, zoom)),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertEquals(2, result.size)
    }

    @Test
    fun `resolveClusterCells도 complete-linkage를 사용해 체인 확장을 막는다`() {
        val zoom = 13
        val base = lonLatToWorldPx(127.0, 37.3, zoom.toDouble())
        val a = worldPxToLonLat(base.first, base.second, zoom.toDouble())
        val b = worldPxToLonLat(base.first + 40.0, base.second, zoom.toDouble())
        val c = worldPxToLonLat(base.first + 80.0, base.second, zoom.toDouble())

        val photosByCell =
            mapOf(
                CellCoord(10, 10) to listOf(GeoPoint(a.first, a.second)),
                CellCoord(11, 10) to listOf(GeoPoint(b.first, b.second)),
                CellCoord(12, 10) to listOf(GeoPoint(c.first, c.second)),
            )

        val merged = strategy.resolveClusterCells(zoom, photosByCell, CellCoord(10, 10))

        assertTrue(CellCoord(10, 10) in merged)
        assertTrue(CellCoord(11, 10) in merged)
        assertTrue(CellCoord(12, 10) !in merged)
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

        assertEquals(2, result.size)
        assertTrue(result.any { it.count == 3 })
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
                ClusterResponse("z14_24681_7832", 1, "a.jpg", origin.first, origin.second),
                ClusterResponse("z14_24681_7832", 2, "b.jpg", farEnough.first, farEnough.second),
            )

        val result = strategy.mergeClusters(clusters, zoom)

        assertEquals(2, result.size)
        assertTrue(result.map { it.clusterId }.toSet().size == 2)
        assertTrue(result.any { it.clusterId == "z14_24681_7832" })
        assertTrue(result.any { it.clusterId == "z14_24681_7832_g2" })
    }

    private fun cluster(
        clusterId: String,
        lonLat: Pair<Double, Double>,
    ): ClusterResponse =
        ClusterResponse(
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
