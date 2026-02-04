package kr.co.lokit.api.common.permission

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.domain.album.application.port.AlbumRepositoryPort
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import kr.co.lokit.api.domain.photo.application.port.PhotoRepositoryPort
import kr.co.lokit.api.domain.user.application.port.UserRepositoryPort
import kr.co.lokit.api.fixture.createAlbum
import kr.co.lokit.api.fixture.createCouple
import kr.co.lokit.api.fixture.createPhoto
import kr.co.lokit.api.fixture.createUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PermissionServiceTest {

    @Mock
    lateinit var coupleRepository: CoupleRepositoryPort

    @Mock
    lateinit var albumRepository: AlbumRepositoryPort

    @Mock
    lateinit var photoRepository: PhotoRepositoryPort

    @Mock
    lateinit var userRepository: UserRepositoryPort

    @InjectMocks
    lateinit var permissionService: PermissionService

    @Test
    fun `Admin 사용자는 isAdmin이 true를 반환한다`() {
        val adminUser = createUser(id = 1L, role = UserRole.ADMIN)
        `when`(userRepository.findById(1L)).thenReturn(adminUser)

        val result = permissionService.isAdmin(1L)

        assert(result)
    }

    @Test
    fun `일반 사용자는 isAdmin이 false를 반환한다`() {
        val user = createUser(id = 1L, role = UserRole.USER)
        `when`(userRepository.findById(1L)).thenReturn(user)

        val result = permissionService.isAdmin(1L)

        assert(!result)
    }

    @Test
    fun `커플 멤버는 커플에 접근할 수 있다`() {
        val user = createUser(id = 1L)
        val couple = createCouple(id = 1L, userIds = listOf(1L, 2L))
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(coupleRepository.findById(1L)).thenReturn(couple)

        assert(permissionService.isCoupleMember(1L, 1L))
    }

    @Test
    fun `커플 비멤버는 커플에 접근할 수 없다`() {
        val user = createUser(id = 3L)
        val couple = createCouple(id = 1L, userIds = listOf(1L, 2L))
        `when`(userRepository.findById(3L)).thenReturn(user)
        `when`(coupleRepository.findById(1L)).thenReturn(couple)

        assert(!permissionService.isCoupleMember(3L, 1L))
    }

    @Test
    fun `Admin은 커플 멤버십 체크를 통과한다`() {
        val adminUser = createUser(id = 1L, role = UserRole.ADMIN)
        `when`(userRepository.findById(1L)).thenReturn(adminUser)

        assert(permissionService.isCoupleMember(1L, 999L))
    }

    @Test
    fun `앨범 생성자는 앨범을 수정할 수 있다`() {
        val user = createUser(id = 1L, role = UserRole.USER)
        val album = createAlbum(id = 1L, createdById = 1L)
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)

        assert(permissionService.canModifyAlbum(1L, 1L))
    }

    @Test
    fun `앨범 생성자가 아니면 앨범을 수정할 수 없다`() {
        val user = createUser(id = 2L, role = UserRole.USER)
        val album = createAlbum(id = 1L, createdById = 1L)
        `when`(userRepository.findById(2L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)

        assert(!permissionService.canModifyAlbum(2L, 1L))
    }

    @Test
    fun `앨범 생성자는 앨범을 삭제할 수 있다`() {
        val user = createUser(id = 1L, role = UserRole.USER)
        val album = createAlbum(id = 1L, createdById = 1L)
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)

        assert(permissionService.canDeleteAlbum(1L, 1L))
    }

    @Test
    fun `앨범 생성자가 아니면 앨범을 삭제할 수 없다`() {
        val user = createUser(id = 2L, role = UserRole.USER)
        val album = createAlbum(id = 1L, createdById = 1L)
        `when`(userRepository.findById(2L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)

        assert(!permissionService.canDeleteAlbum(2L, 1L))
    }

    @Test
    fun `사진 업로더는 사진을 수정할 수 있다`() {
        val user = createUser(id = 1L, role = UserRole.USER)
        val photo = createPhoto(id = 1L, uploadedById = 1L)
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(photoRepository.findById(1L)).thenReturn(photo)

        assert(permissionService.canModifyPhoto(1L, 1L))
    }

    @Test
    fun `사진 업로더가 아니면 사진을 수정할 수 없다`() {
        val user = createUser(id = 2L, role = UserRole.USER)
        val photo = createPhoto(id = 1L, uploadedById = 1L)
        `when`(userRepository.findById(2L)).thenReturn(user)
        `when`(photoRepository.findById(1L)).thenReturn(photo)

        assert(!permissionService.canModifyPhoto(2L, 1L))
    }

    @Test
    fun `사진 업로더는 사진을 삭제할 수 있다`() {
        val user = createUser(id = 1L, role = UserRole.USER)
        val photo = createPhoto(id = 1L, uploadedById = 1L)
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(photoRepository.findById(1L)).thenReturn(photo)

        assert(permissionService.canDeletePhoto(1L, 1L))
    }

    @Test
    fun `사진 업로더가 아니면 사진을 삭제할 수 없다`() {
        val user = createUser(id = 2L, role = UserRole.USER)
        val photo = createPhoto(id = 1L, uploadedById = 1L)
        `when`(userRepository.findById(2L)).thenReturn(user)
        `when`(photoRepository.findById(1L)).thenReturn(photo)

        assert(!permissionService.canDeletePhoto(2L, 1L))
    }

    @Test
    fun `커플 멤버는 앨범을 읽을 수 있다`() {
        val user = createUser(id = 1L)
        val album = createAlbum(id = 1L, coupleId = 1L)
        val couple = createCouple(id = 1L, userIds = listOf(1L))
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)
        `when`(coupleRepository.findById(1L)).thenReturn(couple)

        assert(permissionService.canAccessAlbum(1L, 1L))
    }

    @Test
    fun `커플 비멤버는 앨범을 읽을 수 없다`() {
        val user = createUser(id = 2L)
        val album = createAlbum(id = 1L, coupleId = 1L)
        val couple = createCouple(id = 1L, userIds = listOf(1L))
        `when`(userRepository.findById(2L)).thenReturn(user)
        `when`(albumRepository.findById(1L)).thenReturn(album)
        `when`(coupleRepository.findById(1L)).thenReturn(couple)

        assert(!permissionService.canAccessAlbum(2L, 1L))
    }

    @Test
    fun `Admin은 앨범을 읽을 수 있다`() {
        val adminUser = createUser(id = 1L, role = UserRole.ADMIN)
        `when`(userRepository.findById(1L)).thenReturn(adminUser)

        assert(permissionService.canAccessAlbum(1L, 999L))
    }

    @Test
    fun `커플 멤버는 사진을 읽을 수 있다`() {
        val user = createUser(id = 1L)
        val photo = createPhoto(id = 1L, albumId = 1L)
        val album = createAlbum(id = 1L, coupleId = 1L)
        val couple = createCouple(id = 1L, userIds = listOf(1L))
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(photoRepository.findById(1L)).thenReturn(photo)
        `when`(albumRepository.findById(1L)).thenReturn(album)
        `when`(coupleRepository.findById(1L)).thenReturn(couple)

        assert(permissionService.canReadPhoto(1L, 1L))
    }

    @Test
    fun `Admin은 사진을 읽을 수 있다`() {
        val adminUser = createUser(id = 1L, role = UserRole.ADMIN)
        `when`(userRepository.findById(1L)).thenReturn(adminUser)

        assert(permissionService.canReadPhoto(1L, 999L))
    }
}
