package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.application.MapService
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createPhoto
import kr.co.lokit.api.fixture.createUser
import kr.co.lokit.api.fixture.createWorkspace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class TempLoginServiceTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Mock
    lateinit var photoRepository: PhotoRepository

    @Mock
    lateinit var albumService: AlbumService

    @Mock
    lateinit var workSpaceService: WorkspaceService

    @Mock
    lateinit var albumBoundsService: AlbumBoundsService

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var mapService: MapService

    lateinit var tempLoginService: TempLoginService

    @BeforeEach
    fun setUp() {
        tempLoginService = TempLoginService(
            photoRepository = photoRepository,
            albumService = albumService,
            workSpaceService = workSpaceService,
            albumBoundsService = albumBoundsService,
            userRepository = userRepository,
            mapService = mapService,
            region = "ap-northeast-2",
            bucket = "test-bucket",
        )
    }

    @Test
    fun `신규 유저로 로그인하면 워크스페이스, 앨범, 사진이 생성된다`() {
        val user = createUser(email = "new@test.com")
        val savedUser = createUser(id = 1L, email = "new@test.com")
        val workspace = createWorkspace(id = 1L, name = "ws12345")
        val album = createAlbum(id = 1L, workspaceId = 1L)
        val photos = (1L..9L).map { createPhoto(id = it, albumId = 1L, uploadedById = 1L) }
        val albumMapInfo = AlbumMapInfoResponse(
            albumId = 1L,
            centerLongitude = 127.0,
            centerLatitude = 37.5,
            boundingBox = null,
        )

        doReturn(null).`when`(userRepository).findByEmail("new@test.com")
        doReturn(savedUser).`when`(userRepository).save(anyObject())
        doReturn(workspace).`when`(workSpaceService).create(anyObject(), anyLong())
        doReturn(album).`when`(albumService).create(anyObject())
        doReturn(photos).`when`(photoRepository).saveAll(anyObject())
        doReturn(albumMapInfo).`when`(mapService).getAlbumMapInfo(1L)

        val result = tempLoginService.login(user)

        assertEquals(1L, result.userId)
        assertEquals(1L, result.workspaceId)
        assertEquals(1L, result.albumId)
        assertEquals(9, result.photos.size)
        assertNotNull(result.albumLocation)
    }

    @Test
    fun `기존 유저로 로그인하면 새 워크스페이스와 앨범이 생성된다`() {
        val existingUser = createUser(id = 5L, email = "existing@test.com")
        val workspace = createWorkspace(id = 2L, name = "ws99999")
        val album = createAlbum(id = 2L, workspaceId = 2L)
        val photos = (1L..9L).map { createPhoto(id = it, albumId = 2L, uploadedById = 5L) }
        val albumMapInfo = AlbumMapInfoResponse(
            albumId = 2L,
            centerLongitude = 127.0,
            centerLatitude = 37.5,
            boundingBox = null,
        )

        doReturn(existingUser).`when`(userRepository).findByEmail("existing@test.com")
        doReturn(workspace).`when`(workSpaceService).create(anyObject(), anyLong())
        doReturn(album).`when`(albumService).create(anyObject())
        doReturn(photos).`when`(photoRepository).saveAll(anyObject())
        doReturn(albumMapInfo).`when`(mapService).getAlbumMapInfo(2L)

        val result = tempLoginService.login(existingUser)

        assertEquals(5L, result.userId)
        assertEquals(2L, result.workspaceId)
        assertEquals(2L, result.albumId)
        assertEquals(9, result.photos.size)
    }
}
