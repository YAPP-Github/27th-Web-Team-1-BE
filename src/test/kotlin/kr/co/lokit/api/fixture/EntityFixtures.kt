package kr.co.lokit.api.fixture

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.common.entity.BaseEntity
import kr.co.lokit.api.common.util.InviteCodeGenerator
import kr.co.lokit.api.config.security.UserPrincipal
import kr.co.lokit.api.domain.album.infrastructure.AlbumEntity
import kr.co.lokit.api.domain.couple.infrastructure.CoupleEntity
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.map.infrastructure.AlbumBoundsEntity
import kr.co.lokit.api.domain.photo.infrastructure.CommentEntity
import kr.co.lokit.api.domain.photo.infrastructure.EmoticonEntity
import kr.co.lokit.api.domain.photo.infrastructure.PhotoEntity
import kr.co.lokit.api.domain.user.infrastructure.RefreshTokenEntity
import kr.co.lokit.api.domain.user.infrastructure.UserEntity
import org.locationtech.jts.geom.Point
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.time.LocalDate
import java.time.LocalDateTime

private fun setEntityId(
    entity: BaseEntity,
    id: Long,
) {
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

fun createCoupleEntity(
    id: Long? = null,
    name: String = "테스트",
): CoupleEntity {
    val entity = CoupleEntity(name = name, inviteCode = InviteCodeGenerator.generate())
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createAlbumEntity(
    id: Long? = null,
    title: String = "여행",
    couple: CoupleEntity = createCoupleEntity(),
    createdBy: UserEntity = createUserEntity(),
): AlbumEntity {
    val entity = AlbumEntity(title = title, couple = couple, createdBy = createdBy)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createPhotoEntity(
    id: Long? = null,
    url: String = "https://example.com/photo.jpg",
    takenAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 12, 0),
    album: AlbumEntity = createAlbumEntity(),
    location: Point = PhotoEntity.createPoint(127.0, 37.5),
    address: String = "서울 강남구",
    uploadedBy: UserEntity = createUserEntity(),
): PhotoEntity {
    val entity =
        PhotoEntity(
            url = url,
            takenAt = takenAt,
            album = album,
            location = location,
            address = address,
            uploadedBy = uploadedBy,
        )
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createAlbumBoundsEntity(
    id: Long? = null,
    albumId: Long = 1L,
    idType: BoundsIdType = BoundsIdType.ALBUM,
    minLongitude: Double = 127.0,
    maxLongitude: Double = 127.0,
    minLatitude: Double = 37.5,
    maxLatitude: Double = 37.5,
): AlbumBoundsEntity {
    val entity =
        AlbumBoundsEntity(
            standardId = albumId,
            idType = idType,
            minLongitude = minLongitude,
            maxLongitude = maxLongitude,
            minLatitude = minLatitude,
            maxLatitude = maxLatitude,
        )
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createRefreshTokenEntity(
    id: Long? = null,
    token: String = "test-refresh-token",
    user: UserEntity = createUserEntity(id = 1L),
    expiresAt: LocalDateTime = LocalDateTime.now().plusDays(7),
): RefreshTokenEntity {
    val entity = RefreshTokenEntity(token = token, user = user, expiresAt = expiresAt)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createCommentEntity(
    id: Long? = null,
    photo: PhotoEntity = createPhotoEntity(),
    user: UserEntity = createUserEntity(),
    content: String = "테스트 댓글",
): CommentEntity {
    val entity = CommentEntity(photo = photo, user = user, content = content, commentedAt = LocalDate.now())
    id?.let { setEntityId(entity, it) }
    return entity
}

fun createEmoticonEntity(
    id: Long? = null,
    comment: CommentEntity = createCommentEntity(),
    user: UserEntity = createUserEntity(),
    emoji: String = "😀",
): EmoticonEntity {
    val entity = EmoticonEntity(comment = comment, user = user, emoji = emoji)
    id?.let { setEntityId(entity, it) }
    return entity
}

fun userAuth(
    id: Long = 1L,
    email: String = "test@test.com",
    name: String = "테스트",
    role: UserRole = UserRole.USER,
): UsernamePasswordAuthenticationToken {
    val userPrincipal = UserPrincipal(id = id, email = email, name = name, role = role)
    return UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.authorities)
}
