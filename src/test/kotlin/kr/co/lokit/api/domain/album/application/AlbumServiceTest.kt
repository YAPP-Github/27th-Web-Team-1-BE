package kr.co.lokit.api.domain.album.application

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.map.application.MapPhotosCacheService
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createCouple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.cache.CacheManager
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AlbumServiceTest {
    @Mock
    lateinit var albumRepository: AlbumRepositoryPort

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var currentCoupleAlbumResolver: CurrentCoupleAlbumResolver

    @Mock
    lateinit var mapPhotosCacheService: MapPhotosCacheService

    @Mock
    lateinit var cacheManager: CacheManager

    @InjectMocks
    lateinit var albumCommandService: AlbumCommandService

    @InjectMocks
    lateinit var albumQueryService: AlbumQueryService

    @Test
    fun `앨범을 생성할 수 있다`() {
        val album = createAlbum(title = "여행")
        val savedAlbum = createAlbum(id = 1L, title = "여행")
        `when`(currentCoupleAlbumResolver.requireCurrentCoupleId(1L)).thenReturn(1L)
        `when`(currentCoupleAlbumResolver.requireDefaultAlbum(1L)).thenReturn(createAlbum(coupleId = 1L, isDefault = true))
        `when`(albumRepository.existsByCoupleIdAndTitle(1L, "여행")).thenReturn(false)
        `when`(albumRepository.save(album, 1L)).thenReturn(savedAlbum)

        val result = albumCommandService.create(album, 1L)

        assertEquals(1L, result.id)
        assertEquals("여행", result.title)
    }

    @Test
    fun `동일한 이름의 앨범이 있으면 생성할 수 없다`() {
        val album = createAlbum(title = "여행")
        `when`(currentCoupleAlbumResolver.requireCurrentCoupleId(1L)).thenReturn(1L)
        `when`(currentCoupleAlbumResolver.requireDefaultAlbum(1L)).thenReturn(createAlbum(coupleId = 1L, isDefault = true))
        `when`(albumRepository.existsByCoupleIdAndTitle(1L, "여행")).thenReturn(true)

        assertThrows<BusinessException.AlbumAlreadyExistsException> {
            albumCommandService.create(album, 1L)
        }
    }

    @Test
    fun `사용자의 선택 가능한 앨범 목록을 조회할 수 있다`() {
        val albums =
            listOf(
                createAlbum(id = 1L, title = "앨범1"),
                createAlbum(id = 2L, title = "앨범2"),
            )
        `when`(coupleRepository.findByUserId(1L)).thenReturn(createCouple(id = 1L))
        `when`(albumRepository.findAllByCoupleId(1L)).thenReturn(albums)

        val result = albumQueryService.getSelectableAlbums(1L)

        assertEquals(2, result.size)
        assertEquals("앨범1", result[0].title)
        assertEquals("앨범2", result[1].title)
    }

    @Test
    fun `앨범 제목을 수정할 수 있다`() {
        val album = createAlbum(id = 1L, title = "기존 제목", coupleId = 1L, createdById = 1L)
        val updatedAlbum = createAlbum(id = 1L, title = "새 제목", coupleId = 1L, createdById = 1L)
        `when`(albumRepository.findById(1L)).thenReturn(album)
        `when`(albumRepository.existsByCoupleIdAndTitle(1L, "새 제목")).thenReturn(false)
        `when`(albumRepository.update(album.copy(title = "새 제목"))).thenReturn(updatedAlbum)

        val result = albumCommandService.updateTitle(1L, "새 제목", 1L)

        assertEquals("새 제목", result.title)
    }

    @Test
    fun `수정하려는 제목이 이미 존재하면 수정할 수 없다`() {
        val album = createAlbum(id = 1L, title = "기존 제목", coupleId = 1L, createdById = 1L)
        `when`(albumRepository.findById(1L)).thenReturn(album)
        `when`(albumRepository.existsByCoupleIdAndTitle(1L, "중복 제목")).thenReturn(true)

        assertThrows<BusinessException.AlbumAlreadyExistsException> {
            albumCommandService.updateTitle(1L, "중복 제목", 1L)
        }
    }

    @Test
    fun `기본 앨범의 제목은 수정할 수 없다`() {
        val defaultAlbum = createAlbum(id = 1L, title = "default", isDefault = true, createdById = 1L)
        `when`(albumRepository.findById(1L)).thenReturn(defaultAlbum)

        assertThrows<BusinessException.DefaultAlbumTitleChangeNotAllowedException> {
            albumCommandService.updateTitle(1L, "새 제목", 1L)
        }
    }

    @Test
    fun `앨범을 삭제할 수 있다`() {
        val album = createAlbum(id = 1L, title = "여행", createdById = 1L)
        `when`(albumRepository.findById(1L)).thenReturn(album)

        albumCommandService.delete(1L, 1L)

        verify(albumRepository).deleteById(1L)
    }

    @Test
    fun `기본 앨범은 삭제할 수 없다`() {
        val defaultAlbum = createAlbum(id = 1L, title = "default", isDefault = true, createdById = 1L)
        `when`(albumRepository.findById(1L)).thenReturn(defaultAlbum)

        assertThrows<BusinessException.DefaultAlbumDeletionNotAllowedException> {
            albumCommandService.delete(1L, 1L)
        }
    }

    @Test
    fun `기본 앨범이 없으면 앨범을 생성할 수 없다`() {
        val album = createAlbum(title = "여행")
        `when`(currentCoupleAlbumResolver.requireCurrentCoupleId(1L)).thenReturn(1L)
        `when`(currentCoupleAlbumResolver.requireDefaultAlbum(1L)).thenThrow(BusinessException.DefaultAlbumNotFoundForUserException())

        assertThrows<BusinessException.DefaultAlbumNotFoundForUserException> {
            albumCommandService.create(album, 1L)
        }
    }
}
