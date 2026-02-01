package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridValuesConfigTest {

    @Test
    fun `지원되는 줌 레벨에 대해 그리드 크기를 반환한다`() {
        assertEquals(0.015625, GridValues.getGridSize(10))
        assertEquals(0.0078125, GridValues.getGridSize(11))
        assertEquals(0.00390625, GridValues.getGridSize(12))
        assertEquals(0.001953125, GridValues.getGridSize(13))
        assertEquals(0.0009765625, GridValues.getGridSize(14))
    }

    @Test
    fun `지원되지 않는 줌 레벨은 기본값을 반환한다`() {
        assertEquals(0.001953125, GridValues.getGridSize(13))
        assertEquals(0.001953125, GridValues.getGridSize(4))
    }

    @Test
    fun `지원되는 줌 레벨 목록을 반환한다`() {
        val levels = GridValues.getSupportedZoomLevels()

        assertTrue(levels.contains(10))
        assertTrue(levels.contains(14))
        assertEquals(8, levels.size)
    }
}
