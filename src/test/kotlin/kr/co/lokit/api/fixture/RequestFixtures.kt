package kr.co.lokit.api.fixture

import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.couple.dto.CreateCoupleRequest
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PresignedUrlRequest
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest
import kr.co.lokit.api.domain.user.dto.UpdateNicknameRequest
import kr.co.lokit.api.domain.user.dto.UpdateProfileImageRequest
import java.time.LocalDateTime

fun createAlbumRequest(
    title: String = "여행",
) = AlbumRequest(title = title)

fun createPhotoRequest(
    url: String = "https://example.com/photo.jpg",
    albumId: Long = 1L,
    longitude: Double = 127.0,
    latitude: Double = 37.5,
    takenAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 12, 0),
    description: String? = null,
) = CreatePhotoRequest(
    url = url,
    albumId = albumId,
    longitude = longitude,
    latitude = latitude,
    takenAt = takenAt,
    description = description,
)

fun createUpdatePhotoRequest(
    albumId: Long,
    longitude: Double,
    latitude: Double,
    description: String? = null,
) = UpdatePhotoRequest(
    albumId = albumId,
    longitude = longitude,
    latitude = latitude,
    description = description,
)

fun createPresignedUrlRequest(
    fileName: String = "photo.jpg",
    contentType: String = "image/jpeg",
) = PresignedUrlRequest(fileName = fileName, contentType = contentType)

fun createCoupleRequest(
    name: String = "테스트",
) = CreateCoupleRequest(name = name)

fun createJoinCoupleRequest(
    inviteCode: String = "123456",
) = JoinCoupleRequest(inviteCode = inviteCode)

fun createUpdateNicknameRequest(
    nickname: String = "새닉네임",
) = UpdateNicknameRequest(nickname = nickname)

fun createUpdateProfileImageRequest(
    profileImageUrl: String = "https://example.com/profile.jpg",
) = UpdateProfileImageRequest(profileImageUrl = profileImageUrl)
