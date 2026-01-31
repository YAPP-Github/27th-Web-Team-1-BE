package kr.co.lokit.api.fixture

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import kr.co.lokit.api.domain.workspace.infrastructure.WorkspaceEntity
import org.locationtech.jts.geom.Point
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.time.LocalDateTime

private fun setEntityId(entity: BaseEntity, id: Long) {
    val idField = BaseEntity::class.java.getDeclaredField("id")
    idField.isAccessible = true
    idField.set(entity, id)

    val versionField = BaseEntity::class.java.getDeclaredField("version")
    versionField.isAccessible = true
    versionField.set(entity, 0L)
}

fun createUserEntity(
    id: Long? = null,
    email: String = "test@test.com",
    name: String = "테스트",
    role: UserRole = UserRole.USER,
): UserEntity {
    val entity = UserEntity(email = email, name = name, role = role)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createWorkspaceEntity(
    id: Long? = null,
    name: String = "테스트",
): WorkspaceEntity {
    val entity = WorkspaceEntity(name = name)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createAlbumEntity(
    id: Long? = null,
    title: String = "여행",
    workspace: WorkspaceEntity = createWorkspaceEntity(),
): AlbumEntity {
    val entity = AlbumEntity(title = title, workspace = workspace)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createPhotoEntity(
    id: Long? = null,
    url: String = "https://example.com/photo.jpg",
    takenAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 12, 0),
    album: AlbumEntity = createAlbumEntity(),
    location: Point = PhotoEntity.createPoint(127.0, 37.5),
    uploadedBy: UserEntity = createUserEntity(),
): PhotoEntity {
    val entity = PhotoEntity(
        url = url,
        takenAt = takenAt,
        album = album,
        location = location,
        uploadedBy = uploadedBy,
    )
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createAlbumBoundsEntity(
    id: Long? = null,
    albumId: Long = 1L,
    minLongitude: Double = 127.0,
    maxLongitude: Double = 127.0,
    minLatitude: Double = 37.5,
    maxLatitude: Double = 37.5,
): AlbumBoundsEntity {
    val entity = AlbumBoundsEntity(
        standardId = albumId,
        minLongitude = minLongitude,
        maxLongitude = maxLongitude,
        minLatitude = minLatitude,
        maxLatitude = maxLatitude,
    )
    id?.let { setEntityId(entity, it) }
    return entity
}

fun userAuth(
    id: Long = 1L,
    email: String = "test@test.com",
    name: String = "테스트",
    role: UserRole = UserRole.USER,
): UsernamePasswordAuthenticationToken {
    val userEntity = createUserEntity(id = id, email = email, name = name, role = role)
    return UsernamePasswordAuthenticationToken(userEntity, null, userEntity.authorities)
}
