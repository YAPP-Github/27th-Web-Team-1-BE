package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridValuesConfigTest {
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
