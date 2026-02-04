package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.AlbumBoundsRepositoryPort
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.fixture.createAlbumBounds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AlbumBoundsServiceTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Mock
    lateinit var albumBoundsRepository: AlbumBoundsRepositoryPort

    @InjectMocks
    lateinit var albumBoundsService: AlbumBoundsService

    @Test
    fun `바운드가 없을 때 초기 바운드를 생성한다`() {
        `when`(albumBoundsRepository.findByAlbumIdOrNull(1L)).thenReturn(null)
        doReturn(AlbumBounds.createInitial(1L, 127.0, 37.5))
            .`when`(albumBoundsRepository).save(anyObject())

        albumBoundsService.updateBoundsOnPhotoAdd(1L, 127.0, 37.5)

        verify(albumBoundsRepository).save(anyObject())
    }

    @Test
    fun `기존 바운드가 있을 때 확장한다`() {
        val existingBounds = createAlbumBounds(id = 1L)
        `when`(albumBoundsRepository.findByAlbumIdOrNull(1L)).thenReturn(existingBounds)
        doReturn(existingBounds).`when`(albumBoundsRepository).apply(anyObject())

        albumBoundsService.updateBoundsOnPhotoAdd(1L, 128.0, 38.0)

        verify(albumBoundsRepository).apply(anyObject())
    }
}
