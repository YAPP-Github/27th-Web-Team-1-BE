package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClusteringTest {

    @Test
    fun `BBox를 문자열에서 파싱할 수 있다`() {
        val bbox = BBox.fromString("126.9,37.4,127.1,37.6")

        assertEquals(126.9, bbox.west)
        assertEquals(37.4, bbox.south)
        assertEquals(127.1, bbox.east)
        assertEquals(37.6, bbox.north)
    }

    @Test
    fun `잘못된 BBox 문자열은 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            BBox.fromString("126.9,37.4")
        }
    }

    @Test
    fun `ClusterId를 포맷할 수 있다`() {
        val id = ClusterId.format(14, 130234, 38456)

        assertEquals("z14_130234_38456", id)
    }

    @Test
    fun `ClusterId를 파싱할 수 있다`() {
        val cell = ClusterId.parse("z14_130234_38456")

        assertEquals(14, cell.zoom)
        assertEquals(130234L, cell.cellX)
        assertEquals(38456L, cell.cellY)
    }

    @Test
    fun `잘못된 ClusterId 포맷은 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            ClusterId.parse("invalid")
        }
    }

    @Test
    fun `ClusterId 유효성을 검증할 수 있다`() {
        assertTrue(ClusterId.isValid("z14_130234_38456"))
        assertFalse(ClusterId.isValid("invalid"))
        assertFalse(ClusterId.isValid("z14_abc_123"))
    }

    @Test
    fun `GridCell에서 BBox를 계산할 수 있다`() {
        val cell = GridCell(zoom = 14, cellX = 1, cellY = 1)
        val gridSize = GridConfig.getGridSize(14)
        val bbox = cell.toBBox()

        assertEquals(gridSize, bbox.west)
        assertEquals(gridSize, bbox.south)
        assertEquals(gridSize * 2, bbox.east)
        assertEquals(gridSize * 2, bbox.north)
    }

    @Test
    fun `GridCell을 ClusterId로 변환할 수 있다`() {
        val cell = GridCell(zoom = 14, cellX = 100, cellY = 200)

        assertEquals("z14_100_200", cell.toClusterId())
    }
}
