package kr.co.lokit.api.domain.map.infrastructure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class MapRepositoryImplTest {

    @Mock
    lateinit var jdbcTemplate: JdbcTemplate

    @InjectMocks
    lateinit var mapRepositoryImpl: MapRepositoryImpl

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

    @Test
    fun `클러스터 간 썸네일 URL이 중복되지 않는다`() {
        val candidates = listOf(
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 1, cellY = 1, count = 5, thumbnailUrl = "b.jpg", rank = 2),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "a.jpg", rank = 1),
            candidate(cellX = 2, cellY = 2, count = 3, thumbnailUrl = "c.jpg", rank = 2),
        )

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

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

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

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

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

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

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

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

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

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

        val result = mapRepositoryImpl.deduplicateThumbnails(candidates)

        assertEquals(1, result.size)
        assertEquals("a.jpg", result[0].thumbnailUrl)
    }

    @Test
    fun `빈 후보 목록이면 빈 결과를 반환한다`() {
        val result = mapRepositoryImpl.deduplicateThumbnails(emptyList())

        assertTrue(result.isEmpty())
    }
}
