package kr.co.lokit.api.domain.map.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridValuesConfigTest {
    @Test
    fun `지원되는 줌 레벨 목록을 반환한다`() {
        val levels = GridValues.getSupportedZoomLevels()

        assertTrue(levels.contains(0))
        assertTrue(levels.contains(22))
        assertEquals(23, levels.size)
    }
}
