package kr.co.lokit.api.domain.photo.domain

import kr.co.lokit.api.fixture.createPhoto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhotoTest {

    @Test
    fun `정상적으로 사진을 생성할 수 있다`() {
        val photo = createPhoto(
            albumId = 1L,
            location = Location(127.0, 37.5),
            description = "테스트 사진",
        )

        assertEquals(0L, photo.id)
        assertEquals(1L, photo.albumId)
        assertEquals(127.0, photo.location.longitude)
        assertEquals(37.5, photo.location.latitude)
        assertEquals("테스트 사진", photo.description)
    }

    @Test
    fun `기본값이 올바르게 설정된다`() {
        val photo = createPhoto()

        assertEquals(0L, photo.id)
        assertNull(photo.description)
    }

    @Test
    fun `url과 uploadedById를 설정할 수 있다`() {
        val photo = createPhoto(
            url = "https://example.com/photo.jpg",
            uploadedById = 1L,
        )

        assertEquals("https://example.com/photo.jpg", photo.url)
        assertEquals(1L, photo.uploadedById)
    }
}
