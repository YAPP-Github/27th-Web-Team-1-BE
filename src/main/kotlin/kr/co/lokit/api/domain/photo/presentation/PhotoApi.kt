package kr.co.lokit.api.domain.photo.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.dto.PresignedUrlRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Photo", description = "사진 API")
interface PhotoApi {
    @Operation(
        summary = "사진 목록 조회",
        description = "앨범별로 그룹화된 사진 목록을 조회합니다. (카카오/인스타그램 스타일)",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = PhotoListResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
        ],
    )
    fun getPhotos(albumId: Long): PhotoListResponse

    @Operation(
        summary = "Presigned URL 발급",
        description = "S3에 사진을 업로드하기 위한 presigned URL을 발급합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
        ],
    )
    fun getPresignedUrl(
        request: PresignedUrlRequest,
        userId: Long,
    ): PresignedUrl

    @Operation(
        summary = "사진 생성",
        description = "S3에 업로드된 사진 정보를 저장합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "사진 생성 성공"),
            ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음 (ALBUM_001)"),
        ],
    )
    fun create(
        request: CreatePhotoRequest,
        userId: Long,
    ): IdResponse
}
