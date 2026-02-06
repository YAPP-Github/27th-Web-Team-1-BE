package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClusteringTest {
    @Test
    fun `BBox를 문자열에서 파싱할 수 있다`() {
        val bbox = BBox.parseToBBox("126.9,37.4,127.1,37.6")

        assertEquals(126.9, bbox.west)
        assertEquals(37.4, bbox.south)
        assertEquals(127.1, bbox.east)
        assertEquals(37.6, bbox.north)
    }

    @Test
    fun `잘못된 BBox 문자열은 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            BBox.parseToBBox("126.9,37.4")
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
        val gridSize = GridValues.getGridSize(14 - 1)
        val bbox = cell.toBBox()

        assertEquals(gridSize, bbox.west)
        assertEquals(gridSize, bbox.south)
        assertEquals(gridSize * 2, bbox.east)
        assertEquals(gridSize * 2, bbox.north)
    }

    @Test
    fun `BBox를 중심 좌표에서 생성할 수 있다`() {
        val zoom = 14
        val longitude = 127.0
        val latitude = 37.5

        val bbox = BBox.fromCenter(zoom, longitude, latitude)

        // 중심점이 BBox 내에 포함되어야 한다
        assertTrue(bbox.west <= longitude)
        assertTrue(bbox.east >= longitude)
        assertTrue(bbox.south <= latitude)
        assertTrue(bbox.north >= latitude)

        // 그리드 정렬되어야 한다
        val gridSize = GridValues.getGridSize(zoom)
        assertEquals(0.0, bbox.west % gridSize, 1e-10)
        assertEquals(0.0, bbox.south % gridSize, 1e-10)

        // 모바일 세로 화면: 가로가 세로보다 좁아야 한다
        val width = bbox.east - bbox.west
        val height = bbox.north - bbox.south
        assertTrue(width < height, "모바일 세로 화면: 가로($width) < 세로($height)")
    }

    @Test
    fun `GridCell을 ClusterId로 변환할 수 있다`() {
        val cell = GridCell(zoom = 14, cellX = 100, cellY = 200)

        assertEquals("z14_100_200", cell.toClusterId())
    }
}
