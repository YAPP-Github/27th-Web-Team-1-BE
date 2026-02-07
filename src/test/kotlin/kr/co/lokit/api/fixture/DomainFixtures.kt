package kr.co.lokit.api.fixture

import kr.co.lokit.api.common.constant.UserRole
import kr.co.lokit.api.domain.album.domain.Album
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.map.domain.AlbumBounds
import kr.co.lokit.api.domain.map.domain.BoundsIdType
import kr.co.lokit.api.domain.photo.domain.Location
import kr.co.lokit.api.domain.photo.domain.Photo
import kr.co.lokit.api.domain.photo.domain.PhotoDetail
import kr.co.lokit.api.domain.user.domain.User
import java.time.LocalDateTime

fun createUser(
    id: Long = 0L,
    email: String = "test@test.com",
    name: String = "테스트",
    role: UserRole = UserRole.USER,
) = User(id = id, email = email, name = name, role = role)

fun createCouple(
    id: Long = 0L,
    name: String = "테스트",
    inviteCode: String? = null,
    userIds: List<Long> = emptyList(),
) = Couple(id = id, name = name, inviteCode = inviteCode, userIds = userIds)

fun createAlbum(
    id: Long = 0L,
    title: String = "여행",
    coupleId: Long = 1L,
    createdById: Long = 1L,
    photoCount: Int = 0,
    isDefault: Boolean = false,
) = Album(
    id = id,
    title = title,
    coupleId = coupleId,
    createdById = createdById,
    photoCount = photoCount,
    isDefault = isDefault
)

fun createPhoto(
    id: Long = 0L,
    albumId: Long = 1L,
    coupleId: Long? = null,
    location: Location = createLocation(),
    description: String? = null,
    url: String = "https://example.com/photo.jpg",
    uploadedById: Long = 1L,
    takenAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 12, 0),
    address: String? = null,
) = Photo(
    id = id,
    albumId = albumId,
    coupleId = coupleId,
    location = location,
    description = description,
    url = url,
    uploadedById = uploadedById,
    takenAt = takenAt,
    address = address,
)

fun createLocation(
    longitude: Double = 127.0,
    latitude: Double = 37.5,
) = Location(longitude = longitude, latitude = latitude)

fun createPhotoDetail(
    id: Long = 1L,
    url: String = "https://example.com/photo.jpg",
    takenAt: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 12, 0),
    albumName: String = "여행",
    uploaderName: String = "테스트",
    location: Location = createLocation(),
    description: String? = null,
) = PhotoDetail(
    id = id,
    url = url,
    takenAt = takenAt,
    albumName = albumName,
    uploaderName = uploaderName,
    location = location,
    description = description,
)

fun createAlbumBounds(
    id: Long = 0L,
    albumId: Long = 1L,
    idType: BoundsIdType = BoundsIdType.ALBUM,
    minLongitude: Double = 127.0,
    maxLongitude: Double = 127.0,
    minLatitude: Double = 37.5,
    maxLatitude: Double = 37.5,
) = AlbumBounds(
    id = id,
    standardId = albumId,
    idType = idType,
    minLongitude = minLongitude,
    maxLongitude = maxLongitude,
    minLatitude = minLatitude,
    maxLatitude = maxLatitude,
)
