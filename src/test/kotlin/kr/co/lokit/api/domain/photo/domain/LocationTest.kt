package kr.co.lokit.api.domain.photo.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LocationTest {

    @Test
    fun `정상적으로 위치를 생성할 수 있다`() {
        val location = Location(longitude = 127.0276, latitude = 37.4979)

        assertEquals(127.0276, location.longitude)
        assertEquals(37.4979, location.latitude)
    }
}
