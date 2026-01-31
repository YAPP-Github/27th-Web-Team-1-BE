package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.user.infrastructure.UserJpaRepository
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceJpaRepository
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceUserEntity
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createUserEntity
import kr.co.lokit.api.fixture.createWorkspaceEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@Import(AlbumRepositoryImpl::class)
class AlbumRepositoryTest {

    @Autowired
    lateinit var albumRepository: AlbumRepository

    @Autowired
    lateinit var workspaceJpaRepository: WorkspaceJpaRepository

    @Autowired
    lateinit var albumJpaRepository: AlbumJpaRepository

    @Autowired
    lateinit var userJpaRepository: UserJpaRepository

    lateinit var workspace: WorkspaceEntity
    lateinit var user: UserEntity

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(createUserEntity())
        workspace = createWorkspaceEntity()
        val workspaceUser = WorkspaceUserEntity(workspace = workspace, user = user)
        workspace.addUser(workspaceUser)
        workspace = workspaceJpaRepository.save(workspace)
    }

    @Test
    fun `앨범을 저장할 수 있다`() {
        val album = createAlbum(title = "여행", workspaceId = workspace.nonNullId())

        val saved = albumRepository.save(album, user.nonNullId())

        assertNotNull(saved.id)
        assertEquals("여행", saved.title)
        assertEquals(workspace.id, saved.workspaceId)
    }

    @Test
    fun `ID로 앨범을 조회할 수 있다`() {
        val saved = albumRepository.save(createAlbum(title = "여행", workspaceId = workspace.nonNullId()), user.nonNullId())

        val found = albumRepository.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals("여행", found.title)
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() {
        val found = albumRepository.findById(999L)

        assertNull(found)
    }

    @Test
    fun `앨범 제목을 수정할 수 있다`() {
        val saved = albumRepository.save(createAlbum(title = "여행", workspaceId = workspace.nonNullId()), user.nonNullId())

        val updated = albumRepository.applyTitle(saved.id, "새 제목")

        assertEquals("새 제목", updated.title)
        assertEquals(saved.id, updated.id)
    }

    @Test
    fun `존재하지 않는 앨범 제목을 수정하면 예외가 발생한다`() {
        assertThrows<BusinessException.ResourceNotFoundException> {
            albumRepository.applyTitle(999L, "새 제목")
        }
    }

    @Test
    fun `photos가 비어있는 앨범도 findAllByUserId로 조회된다`() {
        val album = AlbumEntity(title = "빈 앨범", workspace = workspace)
        albumJpaRepository.save(album)

        val result = albumRepository.findAllByUserId(user.nonNullId())

        assertEquals(1, result.size)
        assertEquals("빈 앨범", result[0].title)
    }

    @Test
    fun `photos가 있는 앨범과 없는 앨범이 함께 조회된다`() {
        val albumWithPhotos = AlbumEntity(title = "사진 앨범", workspace = workspace)
        albumJpaRepository.save(albumWithPhotos)
        PhotoEntity(
            url = "https://example.com/photo.jpg",
            album = albumWithPhotos,
            location = PhotoEntity.createPoint(127.0, 37.5),
            uploadedBy = user,
        )
        albumJpaRepository.flush()

        val emptyAlbum = AlbumEntity(title = "빈 앨범", workspace = workspace)
        albumJpaRepository.save(emptyAlbum)
        albumJpaRepository.flush()

        val result = albumRepository.findAllByUserId(user.nonNullId())

        assertEquals(2, result.size)
        val titles = result.map { it.title }.toSet()
        assertTrue(titles.contains("사진 앨범"))
        assertTrue(titles.contains("빈 앨범"))
    }

    @Test
    fun `photos가 비어있는 앨범도 findAllWithPhotos로 조회된다`() {
        val album = AlbumEntity(title = "빈 앨범", workspace = workspace)
        albumJpaRepository.save(album)

        val result = albumRepository.findAllWithPhotos()

        assertEquals(1, result.size)
        assertEquals("빈 앨범", result[0].title)
    }
}
