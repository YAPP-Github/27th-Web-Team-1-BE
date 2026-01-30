package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.domain.album.infrastructure.AlbumRepository
import kr.co.lokit.api.fixture.createAlbum
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AlbumServiceTest {

    @Mock
    lateinit var albumRepository: AlbumRepository

    @InjectMocks
    lateinit var albumService: AlbumService

    @Test
    fun `앨범을 생성할 수 있다`() {
        val album = createAlbum(title = "여행")
        val savedAlbum = createAlbum(id = 1L, title = "여행")
        `when`(albumRepository.save(album)).thenReturn(savedAlbum)

        val result = albumService.create(album)

        assertEquals(1L, result.id)
        assertEquals("여행", result.title)
    }

    @Test
    fun `사용자의 선택 가능한 앨범 목록을 조회할 수 있다`() {
        val albums = listOf(
            createAlbum(id = 1L, title = "앨범1"),
            createAlbum(id = 2L, title = "앨범2"),
        )
        `when`(albumRepository.findAllByUserId(1L)).thenReturn(albums)

        val result = albumService.getSelectableAlbums(1L)

        assertEquals(2, result.size)
        assertEquals("앨범1", result[0].title)
        assertEquals("앨범2", result[1].title)
    }

    @Test
    fun `앨범 제목을 수정할 수 있다`() {
        val updatedAlbum = createAlbum(id = 1L, title = "새 제목")
        `when`(albumRepository.updateTitle(1L, "새 제목")).thenReturn(updatedAlbum)

        val result = albumService.updateTitle(1L, "새 제목")

        assertEquals("새 제목", result.title)
    }

    @Test
    fun `앨범을 삭제할 수 있다`() {
        albumService.delete(1L)

        verify(albumRepository).deleteById(1L)
    }
}
