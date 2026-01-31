package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.album.application.AlbumService
import kr.co.lokit.api.domain.map.application.AlbumBoundsService
import kr.co.lokit.api.domain.map.application.MapService
import kr.co.lokit.api.domain.photo.infrastructure.PhotoRepository
import kr.co.lokit.api.domain.user.infrastructure.UserRepository
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
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

@ExtendWith(MockitoExtension::class)
class TempLoginServiceTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.ArgumentMatchers.any<T>() as T

    @Mock
    lateinit var photoRepository: PhotoRepository

    @Mock
    lateinit var albumService: AlbumService

    @Mock
    lateinit var workspaceService: WorkspaceService

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
            userRepository = userRepository,
            workspaceService = workspaceService,
        )
    }

    @Test
    fun `신규 유저로 로그인하면 워크스페이스가 생성된다`() {
        val user = createUser(email = "new@test.com")
        val savedUser = createUser(id = 1L, email = "new@test.com")
        val workspace = createWorkspace(id = 1L, name = "ws12345")

        doReturn(null).`when`(userRepository).findByEmail("new@test.com")
        doReturn(savedUser).`when`(userRepository).save(anyObject())
        doReturn(workspace).`when`(workspaceService).createIfNone(anyObject(), anyLong())

        val result = tempLoginService.login(user.email)

        assertEquals(1L, result)
    }
}
