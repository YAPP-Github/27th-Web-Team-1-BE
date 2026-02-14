package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.domain.BBox
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MapCacheKeyFactoryTest {
    @Test
    fun `cell key parse는 생성한 key를 복원한다`() {
        val key = MapCacheKeyFactory.buildCellKey(14, 100, -200, 3L, 4L, 9L)

        val parsed = MapCacheKeyFactory.parseCellKey(key)

        assertNotNull(parsed)
        assertEquals(14, parsed.zoom)
        assertEquals(100, parsed.cellX)
        assertEquals(-200, parsed.cellY)
        assertEquals(3L, parsed.coupleId)
        assertEquals(4L, parsed.albumId)
        assertEquals(9L, parsed.version)
    }

    @Test
    fun `individual key parse는 생성한 key를 복원한다`() {
        val bbox = BBox(126.912345, 37.456789, 127.198765, 37.698765)
        val key = MapCacheKeyFactory.buildIndividualKey(bbox, 17, 1L, null, 5L)

        val parsed = MapCacheKeyFactory.parseIndividualKey(key)

        assertNotNull(parsed)
        assertEquals(1L, parsed.coupleId)
        assertEquals(0L, parsed.albumId)
        assertEquals(126.912345, parsed.west, 1e-6)
        assertEquals(37.456789, parsed.south, 1e-6)
        assertEquals(127.198765, parsed.east, 1e-6)
        assertEquals(37.698765, parsed.north, 1e-6)
    }

    @Test
    fun `parseCellKey는 형식이 잘못된 문자열이면 null을 반환한다`() {
        assertNull(MapCacheKeyFactory.parseCellKey("invalid"))
    }

    @Test
    fun `parseIndividualKey는 형식이 잘못된 문자열이면 null을 반환한다`() {
        assertNull(MapCacheKeyFactory.parseIndividualKey("ind_z17_wrong"))
    }

    @Test
    fun `buildRequestStateKey는 album null을 0으로 정규화한다`() {
        val key = MapCacheKeyFactory.buildRequestStateKey(15, 7L, null)

        assertEquals("z15_c7_a0", key)
    }
}
