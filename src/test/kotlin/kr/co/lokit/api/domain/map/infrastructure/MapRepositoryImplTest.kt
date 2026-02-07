package kr.co.lokit.api.domain.map.infrastructure

import kr.co.lokit.api.domain.map.application.port.ClusterCandidate
import kr.co.lokit.api.domain.map.application.port.ClusterData
import kr.co.lokit.api.domain.map.application.port.GridKey
import kr.co.lokit.api.domain.map.application.port.RankedPhoto
import kr.co.lokit.api.domain.map.application.port.UniquePhotoRecord
import kr.co.lokit.api.infrastructure.exposed.ClusteringPipeline
import kr.co.lokit.api.infrastructure.exposed.toClusterProjections
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapRepositoryImplTest {

    private fun candidate(
        cellX: Long,
        cellY: Long,
        count: Int,
        thumbnailUrl: String,
        rank: Int,
    ) = ClusterCandidate(
        cellX = cellX,
        cellY = cellY,
        count = count,
        thumbnailUrl = thumbnailUrl,
        centerLongitude = 127.0,
        centerLatitude = 37.5,
        rank = rank,
    )

    private fun uniquePhoto(
        id: Long,
        url: String,
        cellX: Long,
        cellY: Long,
        takenAt: LocalDateTime = LocalDateTime.now(),
    ) = UniquePhotoRecord(
        id = id,
        url = url,
        longitude = 127.0,
        latitude = 37.5,
        cellX = cellX,
        cellY = cellY,
        takenAt = takenAt,
    )

    @Test
    fun `클러스터 간 썸네일 URL이 중복되지 않는다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "b.jpg", rank = 2),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "c.jpg", rank = 2),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        val urls = result.map { it.thumbnailUrl }
        assertEquals(urls.toSet().size, urls.size)
    }

    @Test
    fun `count가 높은 클러스터가 우선적으로 썸네일을 선점한다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 10, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 1, cellY = 1, count = 10, thumbnailUrl = "b.jpg", rank = 2),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "c.jpg", rank = 2),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        val cluster1 = result.first { it.cellX == 1L }
        val cluster2 = result.first { it.cellX == 2L }
        assertEquals("a.jpg", cluster1.thumbnailUrl)
        assertEquals("c.jpg", cluster2.thumbnailUrl)
    }

    @Test
    fun `모든 후보가 이미 사용된 경우 첫 번째 후보로 폴백한다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "a.jpg", rank = 1),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        val cluster1 = result.first { it.cellX == 1L }
        val cluster2 = result.first { it.cellX == 2L }
        assertEquals("a.jpg", cluster1.thumbnailUrl)
        assertEquals("a.jpg", cluster2.thumbnailUrl)
    }

    @Test
    fun `결과는 count 내림차순으로 정렬된다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 3, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 10, thumbnailUrl = "b.jpg", rank = 1),
            candidate(cellX = 3, cellY = 3, count = 7, thumbnailUrl = "c.jpg", rank = 1),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        assertEquals(listOf(10, 7, 3), result.map { it.count })
    }

    @Test
    fun `여러 클러스터가 동일한 URL을 공유할 때 각각 다른 썸네일을 선택한다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "b.jpg", rank = 2),
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "c.jpg", rank = 3),
            candidate(cellX = 2, cellY = 2, count = 4, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 4, thumbnailUrl = "d.jpg", rank = 2),
            candidate(cellX = 3, cellY = 3, count = 3, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 3, cellY = 3, count = 3, thumbnailUrl = "b.jpg", rank = 2),
            candidate(cellX = 3, cellY = 3, count = 3, thumbnailUrl = "e.jpg", rank = 3),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        val urls = result.map { it.thumbnailUrl }
        assertEquals(urls.toSet().size, urls.size)
        assertEquals("a.jpg", result.first { it.cellX == 1L }.thumbnailUrl)
        assertEquals("d.jpg", result.first { it.cellX == 2L }.thumbnailUrl)
        assertEquals("b.jpg", result.first { it.cellX == 3L }.thumbnailUrl)
    }

    @Test
    fun `클러스터가 하나일 때 정상 동작한다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "a.jpg", rank = 1),
        )

        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(candidates)

        assertEquals(1, result.size)
        assertEquals("a.jpg", result[0].thumbnailUrl)
    }

    @Test
    fun `빈 후보 목록이면 빈 결과를 반환한다`() {
        val result = ClusteringPipeline.deduplicateThumbnailsFromCandidates(emptyList())

        assertTrue(result.isEmpty())
    }

    // --- 새로운 함수형 파이프라인 테스트 ---

    @Test
    fun `groupByGridCell은 같은 셀의 사진을 그룹화한다`() {
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 3, url = "c.jpg", cellX = 2, cellY = 2),
        )

        val result = ClusteringPipeline.groupByGridCell(photos)

        assertEquals(2, result.size)
        assertEquals(2, result[GridKey(1, 1)]?.size)
        assertEquals(1, result[GridKey(2, 2)]?.size)
    }

    @Test
    fun `calculateClusterStats는 올바른 통계를 계산한다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 1, cellY = 1, takenAt = now),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 1, cellY = 1, takenAt = now.minusDays(1)),
        )

        val grouped = ClusteringPipeline.groupByGridCell(photos)
        val result = ClusteringPipeline.calculateClusterStats(grouped)

        assertEquals(1, result.size)
        assertEquals(2, result[0].count)
        assertEquals(2, result[0].photosByRank.size)
        assertEquals("a.jpg", result[0].photosByRank[0].url) // 더 최근 것이 rank 1
        assertEquals(1, result[0].photosByRank[0].rank)
    }

    @Test
    fun `toClusterProjections 확장함수가 전체 파이프라인을 실행한다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 1, cellY = 1, takenAt = now),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 1, cellY = 1, takenAt = now.minusDays(1)),
            uniquePhoto(id = 3, url = "c.jpg", cellX = 2, cellY = 2, takenAt = now),
        )

        val result = photos.toClusterProjections()

        assertEquals(2, result.size)
        assertTrue(result.all { it.thumbnailUrl.isNotEmpty() })
    }

    @Test
    fun `ClusterData로부터 썸네일 중복 제거가 동작한다`() {
        val clusters = listOf(
            ClusterData(
                gridKey = GridKey(1, 1),
                count = 5,
                centerLongitude = 127.0,
                centerLatitude = 37.5,
                photosByRank = listOf(
                    RankedPhoto("shared.jpg", 1),
                    RankedPhoto("unique1.jpg", 2),
                ),
            ),
            ClusterData(
                gridKey = GridKey(2, 2),
                count = 3,
                centerLongitude = 127.0,
                centerLatitude = 37.5,
                photosByRank = listOf(
                    RankedPhoto("shared.jpg", 1),
                    RankedPhoto("unique2.jpg", 2),
                ),
            ),
        )

        val result = ClusteringPipeline.deduplicateThumbnails(clusters)

        assertEquals("shared.jpg", result[0].thumbnailUrl) // count가 높은 클러스터가 선점
        assertEquals("unique2.jpg", result[1].thumbnailUrl) // 다음 후보 선택
    }
}
