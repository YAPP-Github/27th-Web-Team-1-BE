package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.dto.ClusterResponse
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DistanceBasedClusterBoundaryMergeStrategyTest {
    private val strategy = DistanceBasedClusterBoundaryMergeStrategy()

    @Test
    fun `줌 11에서는 인접한 2km 클러스터가 병합되지 않는다`() {
        val result = strategy.mergeClusters(clustersAroundTwoKm(), 11)

        assertEquals(2, result.size)
    }

    @Test
    fun `줌 11에서는 인접한 700m 클러스터가 병합된다`() {
        val result = strategy.mergeClusters(clustersAroundSevenHundredMeters(), 11)

        assertEquals(1, result.size)
        assertEquals(2, result.first().count)
    }

    private fun clustersAroundTwoKm(): List<ClusterResponse> =
        listOf(
            ClusterResponse(
                clusterId = "z13_12330_3904",
                count = 1,
                thumbnailUrl = "a.jpg",
                longitude = 127.0,
                latitude = 37.3,
            ),
            ClusterResponse(
                clusterId = "z13_12330_3905",
                count = 1,
                thumbnailUrl = "b.jpg",
                longitude = 127.0225,
                latitude = 37.3,
            ),
        )

    private fun clustersAroundSevenHundredMeters(): List<ClusterResponse> =
        listOf(
            ClusterResponse(
                clusterId = "z13_12330_3904",
                count = 1,
                thumbnailUrl = "a.jpg",
                longitude = 127.0,
                latitude = 37.3,
            ),
            ClusterResponse(
                clusterId = "z13_12330_3905",
                count = 1,
                thumbnailUrl = "b.jpg",
                longitude = 127.008,
                latitude = 37.3,
            ),
        )
}
