package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.domain.ClusterReadModel
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistanceBasedClusterBoundaryMergeStrategyTest {
    private val strategy = DistanceBasedClusterBoundaryMergeStrategy()

    @Test
    fun `줌 11에서는 인접한 5km 내외 클러스터는 병합되지 않는다`() {
        val result = strategy.mergeClusters(clustersAroundFiveKm(), 11)

        assertEquals(2, result.size)
    }

    @Test
    fun `줌 11에서는 인접한 700m 클러스터가 병합된다`() {
        val result = strategy.mergeClusters(clustersAroundSevenHundredMeters(), 11)

        assertEquals(1, result.size)
        assertEquals(2, result.first().count)
    }

    @Test
    fun `클러스터가 1개면 그대로 반환한다`() {
        val clusters = listOf(clustersAroundSevenHundredMeters().first())

        val result = strategy.mergeClusters(clusters, 11)

        assertEquals(1, result.size)
        assertEquals(clusters.first().clusterId, result.first().clusterId)
    }

    @Test
    fun `clusterId 파싱이 불가능한 항목은 병합 대상에서 제외한다`() {
        val clusters =
            listOf(
                ClusterReadModel("invalid", 1, "a.jpg", 127.0, 37.3),
                ClusterReadModel("z11_100_100", 2, "b.jpg", 127.001, 37.3001),
            )

        val result = strategy.mergeClusters(clusters, 11)

        assertEquals(2, result.size)
    }

    @Test
    fun `대표 셀은 y 이후 x가 가장 작은 셀을 사용한다`() {
        val clusters =
            listOf(
                ClusterReadModel("z11_10_10", 1, "a.jpg", 127.0, 37.3),
                ClusterReadModel("z11_9_10", 1, "b.jpg", 127.0005, 37.3002),
            )

        val result = strategy.mergeClusters(clusters, 11)

        assertEquals(1, result.size)
        assertEquals("z11_9_10", result.first().clusterId)
    }

    @Test
    fun `병합 결과 thumbnail은 takenAt이 가장 최신인 클러스터를 따른다`() {
        val older = LocalDateTime.of(2025, 1, 1, 0, 0)
        val newer = older.plusDays(1)
        val clusters =
            listOf(
                ClusterReadModel("z11_10_10", 3, "old-but-large.jpg", 127.0, 37.3, older),
                ClusterReadModel("z11_10_11", 1, "newest.jpg", 127.0025, 37.3001, newer),
            )

        val result = strategy.mergeClusters(clusters, 11)

        assertEquals(1, result.size)
        assertEquals("newest.jpg", result.first().thumbnailUrl)
        assertEquals(4, result.first().count)
    }

    @Test
    fun `resolveClusterCells는 target이 없으면 target 단독 셀을 반환한다`() {
        val photosByCell = mapOf(CellCoord(1, 1) to listOf(GeoPoint(127.0, 37.0)))

        val result = strategy.resolveClusterCells(11, photosByCell, CellCoord(2, 2))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolveClusterCells는 인접하고 가까운 셀들을 함께 반환한다`() {
        val photosByCell =
            mapOf(
                CellCoord(100, 100) to listOf(GeoPoint(127.0, 37.3), GeoPoint(127.0005, 37.3001)),
                CellCoord(100, 101) to listOf(GeoPoint(127.003, 37.3002)),
            )

        val result = strategy.resolveClusterCells(11, photosByCell, CellCoord(100, 100))

        assertTrue(CellCoord(100, 100) in result)
        assertTrue(CellCoord(100, 101) in result)
    }

    @Test
    fun `resolveClusterCells는 멀리 떨어진 셀을 병합하지 않는다`() {
        val photosByCell =
            mapOf(
                CellCoord(100, 100) to listOf(GeoPoint(127.0, 37.3)),
                CellCoord(100, 101) to listOf(GeoPoint(127.05, 37.3)),
            )

        val result = strategy.resolveClusterCells(11, photosByCell, CellCoord(100, 100))

        assertEquals(setOf(CellCoord(100, 100)), result)
    }

    @Test
    fun `gridSize 보간은 정수 zoom에서 GridValues와 동일하다`() {
        assertEquals(GridValues.getGridSize(12), strategy.getGridSizeAtZoomLevel(12.0), 1e-9)
        assertEquals(GridValues.getGridSize(13), strategy.getGridSizeAtZoomLevel(13.0), 1e-9)
    }

    @Test
    fun `제공된 샘플 좌표는 zoom 12_0에서 병합된다`() {
        val clusters =
            listOf(
                ClusterReadModel(
                    clusterId = "z12_6170_1957",
                    count = 1,
                    thumbnailUrl = "b116862e.jpg",
                    longitude = 127.112588277624,
                    latitude = 37.3602093121085,
                    takenAt = LocalDateTime.of(2025, 1, 1, 10, 0),
                ),
                ClusterReadModel(
                    clusterId = "z12_6170_1958",
                    count = 1,
                    thumbnailUrl = "ee5c09d6.jpg",
                    longitude = 127.108097457244,
                    latitude = 37.3661737923199,
                    takenAt = LocalDateTime.of(2025, 1, 1, 11, 0),
                ),
            )

        val result = strategy.mergeClusters(clusters, 12.0)

        assertEquals(1, result.size)
        assertEquals(2, result.first().count)
        assertEquals("ee5c09d6.jpg", result.first().thumbnailUrl)
    }

    private fun clustersAroundFiveKm(): List<ClusterReadModel> =
        listOf(
            ClusterReadModel(
                clusterId = "z13_12330_3904",
                count = 1,
                thumbnailUrl = "a.jpg",
                longitude = 127.0,
                latitude = 37.3,
            ),
            ClusterReadModel(
                clusterId = "z13_12330_3905",
                count = 1,
                thumbnailUrl = "b.jpg",
                longitude = 127.055,
                latitude = 37.3,
            ),
        )

    private fun clustersAroundSevenHundredMeters(): List<ClusterReadModel> =
        listOf(
            ClusterReadModel(
                clusterId = "z13_12330_3904",
                count = 1,
                thumbnailUrl = "a.jpg",
                longitude = 127.0,
                latitude = 37.3,
            ),
            ClusterReadModel(
                clusterId = "z13_12330_3905",
                count = 1,
                thumbnailUrl = "b.jpg",
                longitude = 127.008,
                latitude = 37.3,
            ),
        )
}
