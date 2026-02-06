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
    fun `GridCell에서 생성된 BBox는 유효한 범위를 가져야 한다`() {
        val cell = GridCell(zoom = 14, cellX = 12345, cellY = 6789)
        val bbox = cell.toBBox()

        assertTrue(bbox.west < bbox.east)
        assertTrue(bbox.south < bbox.north)
        assertTrue(bbox.west in -180.0..180.0)
        assertTrue(bbox.south in -90.0..90.0)
    }

    @Test
    fun `BBox를 중심 좌표에서 생성할 수 있다`() {
        val zoom = 14
        val longitude = 127.0
        val latitude = 37.5

        val bbox = BBox.fromCenter(zoom, longitude, latitude)

        assertTrue(bbox.west <= longitude, "West(${bbox.west})는 경도($longitude)보다 작거나 같아야 함")
        assertTrue(bbox.east >= longitude, "East(${bbox.east})는 경도($longitude)보다 크거나 같아야 함")
        assertTrue(bbox.south <= latitude, "South(${bbox.south})는 위도($latitude)보다 작거나 같아야 함")
        assertTrue(bbox.north >= latitude, "North(${bbox.north})는 위도($latitude)보다 크거나 같아야 함")

        val gridSizeMeters = GridValues.getGridSize(zoom)

        val metersX = longitude * (40075016.68557849 / 360.0)

        val metersY =
            Math.log(Math.tan(Math.toRadians(latitude) / 2.0 + Math.PI / 4.0)) * (40075016.68557849 / (2.0 * Math.PI))

        val westMeters = bbox.west * (40075016.68557849 / 360.0)
        val southMeters =
            Math.log(Math.tan(Math.toRadians(bbox.south) / 2.0 + Math.PI / 4.0)) * (40075016.68557849 / (2.0 * Math.PI))

        val westRemainder = Math.abs(westMeters % gridSizeMeters)
        assertTrue(
            westRemainder < 1e-3 || (gridSizeMeters - westRemainder) < 1e-3,
            "West boundary($westMeters) should be aligned with gridSize($gridSizeMeters)",
        )

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
