package kr.co.lokit.api.domain.album.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AlbumTest {

    @Test
    fun `정상적인 제목으로 앨범을 생성할 수 있다`() {
        val album = Album(title = "여행 앨범", workspaceId = 1L)

        assertEquals("여행 앨범", album.title)
        assertEquals(1L, album.workspaceId)
        assertEquals(0L, album.id)
        assertEquals(0, album.photoCount)
    }

    @Test
    fun `10자 이하의 제목으로 앨범을 생성할 수 있다`() {
        val album = Album(title = "1234567890", workspaceId = 1L)

        assertEquals("1234567890", album.title)
    }

    @Test
    fun `빈 제목으로 앨범을 생성할 수 있다`() {
        val album = Album(title = "", workspaceId = 1L)

        assertEquals("", album.title)
    }

    @Test
    fun `10자 초과 제목으로 앨범을 생성하면 예외가 발생한다`() {
        val exception = assertThrows<IllegalArgumentException> {
            Album(title = "12345678901", workspaceId = 1L)
        }

        assertEquals("앨범 제목은 10자 이내여야 합니다.", exception.message)
    }
}
