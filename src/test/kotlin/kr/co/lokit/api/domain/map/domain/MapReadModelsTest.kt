package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapReadModelsTest {
    @Test
    fun `Clusters는 입력 리스트를 복사해 보관한다`() {
        val source = mutableListOf(ClusterReadModel("c1", 1, "t", 127.0, 37.0))
        val clusters = Clusters.of(source)

        source += ClusterReadModel("c2", 1, "t2", 128.0, 38.0)

        assertEquals(1, clusters.asList().size)
        assertEquals("c1", clusters.asList().first().clusterId)
    }

    @Test
    fun `빈 컬렉션 팩토리는 isEmpty true를 반환한다`() {
        assertTrue(Clusters.empty().isEmpty())
        assertTrue(MapPhotos.empty().isEmpty())
        assertTrue(ClusterPhotos.empty().isEmpty())
        assertTrue(Places.empty().isEmpty())
        assertTrue(AlbumThumbnails.empty().isEmpty())
    }

    @Test
    fun `ThumbnailUrls는 리스트를 유지한다`() {
        val urls = ThumbnailUrls.of(listOf("a", "b"))

        assertEquals(listOf("a", "b"), urls.asList())
    }
}
