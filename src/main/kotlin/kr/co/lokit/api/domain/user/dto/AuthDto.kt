package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.co.lokit.api.domain.map.dto.AlbumMapInfoResponse
import kr.co.lokit.api.domain.user.domain.User

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,
) {
    fun toDomain(): User =
        User(
            email = email,
            name = "user",
        )
}

@Schema(description = "로그인 응답")
data class LoginResponse(
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @Schema(description = "워크스페이스 ID", example = "1")
    val workspaceId: Long,
    @Schema(description = "앨범 ID", example = "1")
    val albumId: Long,
    @Schema(description = "사진 목록")
    val photos: List<LoginPhotoResponse>,
    @Schema(description = "앨범 위치 정보")
    val albumLocation: AlbumMapInfoResponse,
)

@Schema(description = "로그인 사진 응답")
data class LoginPhotoResponse(
    @Schema(description = "사진 ID", example = "1")
    val photoId: Long,
    @Schema(description = "사진 URL", example = "https://example.com/photo.jpg")
    val url: String,
    @Schema(description = "경도", example = "127.0")
    val longitude: Double?,
    @Schema(description = "위도", example = "37.5")
    val latitude: Double?,
    @Schema(description = "사진 설명", example = "바다 풍경")
    val description: String?,
)

@Schema(description = "리프레시 토큰 요청")
data class RefreshTokenRequest(
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val refreshToken: String,
)

@Schema(description = "JWT 토큰 응답")
data class JwtTokenResponse(
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val refreshToken: String,
)
