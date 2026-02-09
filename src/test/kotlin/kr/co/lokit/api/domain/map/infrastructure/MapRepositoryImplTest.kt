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

    // --- groupByGridCell 엣지 케이스 ---

    @Test
    fun `groupByGridCell에 빈 리스트를 전달하면 빈 맵을 반환한다`() {
        val result = ClusteringPipeline.groupByGridCell(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `groupByGridCell에 사진이 하나일 때 정상 동작한다`() {
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 5, cellY = 10),
        )

        val result = ClusteringPipeline.groupByGridCell(photos)

        assertEquals(1, result.size)
        assertEquals(1, result[GridKey(5, 10)]?.size)
    }

    @Test
    fun `groupByGridCell에서 같은 셀에 여러 사진이 있으면 하나의 그룹으로 묶인다`() {
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 3, url = "c.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 4, url = "d.jpg", cellX = 1, cellY = 1),
            uniquePhoto(id = 5, url = "e.jpg", cellX = 1, cellY = 1),
        )

        val result = ClusteringPipeline.groupByGridCell(photos)

        assertEquals(1, result.size)
        assertEquals(5, result[GridKey(1, 1)]?.size)
    }

    // --- calculateClusterStats 엣지 케이스 ---

    @Test
    fun `calculateClusterStats에 빈 맵을 전달하면 빈 리스트를 반환한다`() {
        val result = ClusteringPipeline.calculateClusterStats(emptyMap())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculateClusterStats에서 대표 사진은 가장 최근 촬영일 기준이다`() {
        val oldest = LocalDateTime.of(2024, 1, 1, 0, 0)
        val middle = LocalDateTime.of(2025, 6, 1, 0, 0)
        val newest = LocalDateTime.of(2026, 1, 1, 0, 0)

        val photos = listOf(
            uniquePhoto(id = 1, url = "old.jpg", cellX = 1, cellY = 1, takenAt = oldest),
            uniquePhoto(id = 2, url = "mid.jpg", cellX = 1, cellY = 1, takenAt = middle),
            uniquePhoto(id = 3, url = "new.jpg", cellX = 1, cellY = 1, takenAt = newest),
        )

        val grouped = ClusteringPipeline.groupByGridCell(photos)
        val result = ClusteringPipeline.calculateClusterStats(grouped)

        assertEquals(1, result.size)
        assertEquals(newest, result[0].takenAt)
        assertEquals("new.jpg", result[0].photosByRank[0].url)
        assertEquals(1, result[0].photosByRank[0].rank)
    }

    @Test
    fun `calculateClusterStats에서 photosByRank는 최신순으로 정렬된다`() {
        val t1 = LocalDateTime.of(2024, 1, 1, 0, 0)
        val t2 = LocalDateTime.of(2025, 1, 1, 0, 0)
        val t3 = LocalDateTime.of(2026, 1, 1, 0, 0)

        val photos = listOf(
            uniquePhoto(id = 1, url = "c.jpg", cellX = 1, cellY = 1, takenAt = t1),
            uniquePhoto(id = 2, url = "a.jpg", cellX = 1, cellY = 1, takenAt = t3),
            uniquePhoto(id = 3, url = "b.jpg", cellX = 1, cellY = 1, takenAt = t2),
        )

        val grouped = ClusteringPipeline.groupByGridCell(photos)
        val result = ClusteringPipeline.calculateClusterStats(grouped)

        val ranks = result[0].photosByRank
        assertEquals("a.jpg", ranks[0].url) // t3 (최신)
        assertEquals("b.jpg", ranks[1].url) // t2
        assertEquals("c.jpg", ranks[2].url) // t1 (가장 오래됨)
        assertEquals(1, ranks[0].rank)
        assertEquals(2, ranks[1].rank)
        assertEquals(3, ranks[2].rank)
    }

    @Test
    fun `calculateClusterStats에서 여러 그룹은 각각 독립적으로 통계를 계산한다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 1, cellY = 1, takenAt = now),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 1, cellY = 1, takenAt = now.minusDays(1)),
            uniquePhoto(id = 3, url = "c.jpg", cellX = 2, cellY = 2, takenAt = now.minusHours(1)),
        )

        val grouped = ClusteringPipeline.groupByGridCell(photos)
        val result = ClusteringPipeline.calculateClusterStats(grouped)

        assertEquals(2, result.size)
        val group1 = result.first { it.gridKey == GridKey(1, 1) }
        val group2 = result.first { it.gridKey == GridKey(2, 2) }
        assertEquals(2, group1.count)
        assertEquals(1, group2.count)
    }

    @Test
    fun `calculateClusterStats에서 대표 사진의 좌표가 클러스터 중심 좌표로 사용된다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            UniquePhotoRecord(
                id = 1,
                url = "old.jpg",
                longitude = 126.0,
                latitude = 36.0,
                cellX = 1,
                cellY = 1,
                takenAt = now.minusDays(1),
            ),
            UniquePhotoRecord(
                id = 2,
                url = "new.jpg",
                longitude = 128.0,
                latitude = 38.0,
                cellX = 1,
                cellY = 1,
                takenAt = now,
            ),
        )

        val grouped = ClusteringPipeline.groupByGridCell(photos)
        val result = ClusteringPipeline.calculateClusterStats(grouped)

        assertEquals(128.0, result[0].centerLongitude)
        assertEquals(38.0, result[0].centerLatitude)
    }

    // --- deduplicateThumbnails 엣지 케이스 ---

    @Test
    fun `deduplicateThumbnails에 빈 리스트를 전달하면 빈 결과를 반환한다`() {
        val result = ClusteringPipeline.deduplicateThumbnails(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `deduplicateThumbnails에서 사진이 없는 클러스터는 빈 문자열 URL을 반환한다`() {
        val clusters = listOf(
            ClusterData(
                gridKey = GridKey(1, 1),
                count = 0,
                centerLongitude = 127.0,
                centerLatitude = 37.5,
                photosByRank = emptyList(),
            ),
        )

        val result = ClusteringPipeline.deduplicateThumbnails(clusters)

        assertEquals(1, result.size)
        assertEquals("", result[0].thumbnailUrl)
    }

    @Test
    fun `deduplicateThumbnails에서 모든 클러스터의 URL이 고유하면 각자 첫 번째 URL을 사용한다`() {
        val clusters = listOf(
            ClusterData(
                gridKey = GridKey(1, 1),
                count = 5,
                centerLongitude = 127.0,
                centerLatitude = 37.5,
                photosByRank = listOf(RankedPhoto("unique1.jpg", 1)),
            ),
            ClusterData(
                gridKey = GridKey(2, 2),
                count = 3,
                centerLongitude = 127.1,
                centerLatitude = 37.6,
                photosByRank = listOf(RankedPhoto("unique2.jpg", 1)),
            ),
        )

        val result = ClusteringPipeline.deduplicateThumbnails(clusters)

        assertEquals("unique1.jpg", result[0].thumbnailUrl)
        assertEquals("unique2.jpg", result[1].thumbnailUrl)
    }

    // --- toClusterProjections 엣지 케이스 ---

    @Test
    fun `toClusterProjections에 빈 리스트를 전달하면 빈 결과를 반환한다`() {
        val result = emptyList<UniquePhotoRecord>().toClusterProjections()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `toClusterProjections에서 단일 사진은 하나의 클러스터 프로젝션을 반환한다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            uniquePhoto(id = 1, url = "only.jpg", cellX = 1, cellY = 1, takenAt = now),
        )

        val result = photos.toClusterProjections()

        assertEquals(1, result.size)
        assertEquals("only.jpg", result[0].thumbnailUrl)
        assertEquals(1, result[0].count)
        assertEquals(1L, result[0].cellX)
        assertEquals(1L, result[0].cellY)
    }

    @Test
    fun `toClusterProjections에서 같은 셀의 여러 사진은 하나의 클러스터로 합쳐진다`() {
        val now = LocalDateTime.now()
        val photos = listOf(
            uniquePhoto(id = 1, url = "a.jpg", cellX = 3, cellY = 4, takenAt = now),
            uniquePhoto(id = 2, url = "b.jpg", cellX = 3, cellY = 4, takenAt = now.minusDays(1)),
            uniquePhoto(id = 3, url = "c.jpg", cellX = 3, cellY = 4, takenAt = now.minusDays(2)),
        )

        val result = photos.toClusterProjections()

        assertEquals(1, result.size)
        assertEquals(3, result[0].count)
        assertEquals(3L, result[0].cellX)
        assertEquals(4L, result[0].cellY)
    }
}
