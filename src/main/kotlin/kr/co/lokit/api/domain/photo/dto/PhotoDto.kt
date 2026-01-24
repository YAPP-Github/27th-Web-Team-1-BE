package kr.co.lokit.api.domain.photo.dto

import io.swagger.v3.oas.annotations.media.Schema

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
