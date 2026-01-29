package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridConfigTest {

    @Test
    fun `지원되는 줌 레벨에 대해 그리드 크기를 반환한다`() {
        assertEquals(0.015625, GridConfig.getGridSize(10))
        assertEquals(0.0078125, GridConfig.getGridSize(11))
        assertEquals(0.00390625, GridConfig.getGridSize(12))
        assertEquals(0.001953125, GridConfig.getGridSize(13))
        assertEquals(0.0009765625, GridConfig.getGridSize(14))
    }

    @Test
    fun `지원되지 않는 줌 레벨은 기본값을 반환한다`() {
        assertEquals(0.001953125, GridConfig.getGridSize(15))
        assertEquals(0.001953125, GridConfig.getGridSize(5))
    }

    @Test
    fun `지원되는 줌 레벨 목록을 반환한다`() {
        val levels = GridConfig.getSupportedZoomLevels()

        assertTrue(levels.contains(10))
        assertTrue(levels.contains(14))
        assertEquals(5, levels.size)
    }
}
