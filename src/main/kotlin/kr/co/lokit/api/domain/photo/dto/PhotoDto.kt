package kr.co.lokit.api.domain.photo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "위치 정보")
data class LocationResponse(
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
)

@Schema(description = "사진 정보")
data class PhotoResponse(
    @Schema(description = "사진 ID", example = "1")
    val id: Long,
    @Schema(description = "사진 URL", example = "https://example.com/photo.jpg")
    val url: String,
    @Schema(description = "위치 정보")
    val location: LocationResponse,
    @Schema(description = "사진 설명", example = "아름다운 풍경")
    val description: String?,
)

@Schema(description = "앨범별 사진 목록")
data class AlbumWithPhotosResponse(
    @Schema(description = "앨범 ID", example = "1")
    val id: Long,
    @Schema(description = "앨범 제목", example = "여행 앨범")
    val title: String,
    @Schema(description = "사진 개수", example = "10")
    val photoCount: Int,
    @Schema(description = "썸네일 URL", example = "https://example.com/thumbnail.jpg")
    val thumbnailUrl: String?,
    @Schema(description = "앨범 내 사진 목록")
    val photos: List<PhotoResponse>,
)

@Schema(description = "사진 목록 응답 (앨범별 그룹)")
data class PhotoListResponse(
    @Schema(description = "앨범별 사진 목록")
    val albums: List<AlbumWithPhotosResponse>,
)

@Schema(description = "사진 생성 요청")
data class CreatePhotoRequest(
    @Schema(description = "사진 URL", example = "https://bucket.s3.amazonaws.com/photos/1/image.jpg")
    val url: String,
    @Schema(description = "앨범 ID", example = "1")
    val albumId: Long,
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
    @Schema(description = "사진 설명", example = "가족 여행 사진")
    val description: String? = null,
)

@Schema(description = "Presigned URL 응답")
data class PresignedUrl(
    @Schema(description = "S3 업로드용 Presigned URL", example = "https://bucket.s3.amazonaws.com/...?X-Amz-Signature=...")
    val presignedUrl: String,
    @Schema(description = "업로드 완료 후 접근 가능한 객체 URL", example = "https://bucket.s3.amazonaws.com/photos/1/image.jpg")
    val objectUrl: String,
)

@Schema(description = "Presigned URL 요청")
data class PresignedUrlRequest(
    @Schema(description = "파일명", example = "image.jpg")
    val fileName: String,
    @Schema(description = "파일 MIME 타입", example = "image/jpeg")
    val contentType: String,
)
