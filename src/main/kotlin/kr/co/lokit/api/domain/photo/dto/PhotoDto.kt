package kr.co.lokit.api.domain.photo.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

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
    @Schema(description = "사진의 촬영 일시")
    val takenAt: LocalDateTime?,
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
    @field:NotBlank(message = "사진 URL은 필수입니다.")
    @Schema(
        description = "사진 URL",
        example = "https://bucket.s3.amazonaws.com/photos/1/image.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val url: String,
    @Schema(description = "앨범 ID", example = "1 or null(전체 앨범)")
    val albumId: Long?,
    @Schema(description = "사진 경도", example = "127.0276", requiredMode = Schema.RequiredMode.REQUIRED)
    val longitude: Double,
    @Schema(description = "사진 위도", example = "37.4979", requiredMode = Schema.RequiredMode.REQUIRED)
    val latitude: Double,
    @Schema(
        description = "촬영일시 (EXIF 데이터)",
        example = "2026-01-06T14:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val takenAt: LocalDateTime,
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
    @Schema(
        description = "파일명(deprecated, nullable)",
        example = "image.jpg",
    )
    val fileName: String?,
    @field:NotBlank(message = "파일 MIME 타입은 필수입니다.")
    @Schema(description = "파일 MIME 타입", example = "image/jpeg", requiredMode = Schema.RequiredMode.REQUIRED)
    val contentType: String,
)

@Schema(description = "사진 수정 요청")
data class UpdatePhotoRequest(
    @Schema(description = "앨범 ID", example = "1")
    val albumId: Long? = null,
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double? = null,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double? = null,
    @Schema(description = "사진 설명", example = "수정된 사진 설명")
    val description: String? = null,
)

@Schema(description = "사진 상세 정보")
data class PhotoDetailResponse(
    @Schema(description = "사진 ID", example = "1")
    val id: Long,
    @Schema(description = "사진 URL", example = "https://bucket.s3.amazonaws.com/photos/1/image.jpg")
    val url: String,
    @Schema(description = "촬영일시 (ISO 8601 형식)", example = "2026-01-06T14:30:00")
    val takenAt: LocalDateTime?,
    @Schema(description = "앨범명", example = "가족여행")
    val albumName: String,
    @Schema(description = "등록자 이름", example = "홍길동")
    val uploaderName: String,
    @Schema(description = "등록자 프로필 이미지 URL")
    val uploaderProfileImageUrl: String?,
    @Schema(description = "도로명 주소", example = "서울 강남구 테헤란로 123")
    val address: String?,
    @Schema(description = "사진 설명", example = "가족 여행 사진")
    val description: String?,
    @Schema(description = "경도", example = "127.0276")
    val longitude: Double,
    @Schema(description = "위도", example = "37.4979")
    val latitude: Double,
)
