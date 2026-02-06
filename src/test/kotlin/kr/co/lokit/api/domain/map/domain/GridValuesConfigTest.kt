package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridValuesConfigTest {
    @Test
    fun `지원되는 줌 레벨에 대해 그리드 크기를 반환한다`() {
        assertEquals(0.03125, GridValues.getGridSize(10))
        assertEquals(0.015625, GridValues.getGridSize(11))
        assertEquals(0.0078125, GridValues.getGridSize(12))
        assertEquals(0.00390625, GridValues.getGridSize(13))
        assertEquals(0.001953125, GridValues.getGridSize(14))
    }

    @Test
    fun `줌 레벨이 범위를 벗어나면 최소 또는 최대값을 반환한다`() {
        assertEquals(GridValues.getGridSize(0), GridValues.getGridSize(-1))
        assertEquals(GridValues.getGridSize(23), GridValues.getGridSize(25))
    }

    @Test
    fun `지원되는 줌 레벨 목록을 반환한다`() {
        val levels = GridValues.getSupportedZoomLevels()

        assertTrue(levels.contains(0))
        assertTrue(levels.contains(22))
        assertEquals(23, levels.size)
    }
}
