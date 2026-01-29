package kr.co.lokit.api.domain.album.infrastructure

import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@Import(AlbumRepositoryImpl::class)
class AlbumRepositoryTest {

    @Autowired
    lateinit var albumRepository: AlbumRepository

    @Autowired
    lateinit var workspaceJpaRepository: WorkspaceJpaRepository

    @Autowired
    lateinit var albumJpaRepository: AlbumJpaRepository

    lateinit var workspace: WorkspaceEntity

    @BeforeEach
    fun setUp() {
        workspace = workspaceJpaRepository.save(WorkspaceEntity(name = "테스트"))
    }

    @Test
    fun `앨범을 저장할 수 있다`() {
        val album = Album(title = "여행", workspaceId = workspace.id)

        val saved = albumRepository.save(album)

        assertNotNull(saved.id)
        assertEquals("여행", saved.title)
        assertEquals(workspace.id, saved.workspaceId)
    }

    @Test
    fun `ID로 앨범을 조회할 수 있다`() {
        val saved = albumRepository.save(Album(title = "여행", workspaceId = workspace.id))

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
        val saved = albumRepository.save(Album(title = "여행", workspaceId = workspace.id))

        val updated = albumRepository.updateTitle(saved.id, "새 제목")

        assertEquals("새 제목", updated.title)
        assertEquals(saved.id, updated.id)
    }

    @Test
    fun `존재하지 않는 앨범 제목을 수정하면 예외가 발생한다`() {
        assertThrows<BusinessException.ResourceNotFoundException> {
            albumRepository.updateTitle(999L, "새 제목")
        }
    }

    @Test
    fun `앨범을 삭제할 수 있다`() {
        val saved = albumRepository.save(Album(title = "여행", workspaceId = workspace.id))

        albumRepository.deleteById(saved.id)

        assertNull(albumRepository.findById(saved.id))
    }

    @Test
    fun `존재하지 않는 앨범을 삭제하면 예외가 발생한다`() {
        assertThrows<BusinessException.ResourceNotFoundException> {
            albumRepository.deleteById(999L)
        }
    }
}
