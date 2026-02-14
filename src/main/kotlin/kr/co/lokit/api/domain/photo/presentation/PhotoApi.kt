package kr.co.lokit.api.domain.photo.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.photo.dto.CreatePhotoRequest
import kr.co.lokit.api.domain.photo.dto.PhotoDetailResponse
import kr.co.lokit.api.domain.photo.dto.PhotoListResponse
import kr.co.lokit.api.domain.photo.dto.PresignedUrl
import kr.co.lokit.api.domain.photo.dto.PresignedUrlRequest
import kr.co.lokit.api.domain.photo.dto.UpdatePhotoRequest

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
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content()],
            ),
        ],
    )
    fun getPhotos(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "앨범 ID (0이면 전체 조회)", example = "1") albumId: Long,
    ): PhotoListResponse

    @Operation(
        summary = "Presigned URL 발급",
        description = "S3에 사진을 업로드하기 위한 presigned URL을 발급합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
        ],
    )
    fun getPresignedUrl(
        @Parameter(
            name = "X-Idempotency-Key",
            `in` = ParameterIn.HEADER,
            description = "멱등성 보장을 위한 키",
            required = false,
            example = "550e8400-e29b-41d4-a716-446655440000",
        )
        idempotencyKey: String?,
        request: PresignedUrlRequest,
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
        @Parameter(hidden = true) userId: Long,
    ): IdResponse

    @Operation(
        summary = "사진 상세 조회",
        description = """
            사진 ID를 기반으로 사진 상세 정보를 조회합니다.

            - 촬영일, 앨범명, 등록자명, 주소, 설명 포함
            - 주소는 좌표 기반 역지오코딩으로 조회
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = PhotoDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 필요",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "접근 권한 없음",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사진을 찾을 수 없음 (PHOTO_001)",
                content = [Content()],
            ),
        ],
    )
    fun getPhotoDetail(
        @Parameter(hidden = true) userId: Long,
        @Parameter(
            description = "사진 ID",
            example = "1",
            required = true,
        )
        id: Long,
    ): PhotoDetailResponse

    @Operation(
        summary = "사진 수정",
        description = "사진의 설명을 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(responseCode = "403", description = "수정 권한 없음", content = [Content()]),
            ApiResponse(responseCode = "404", description = "사진을 찾을 수 없음 (PHOTO_001)", content = [Content()]),
        ],
    )
    fun update(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "사진 ID", example = "1", required = true)
        id: Long,
        request: UpdatePhotoRequest,
    ): IdResponse

    @Operation(
        summary = "사진 삭제",
        description = "사진을 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(responseCode = "403", description = "삭제 권한 없음", content = [Content()]),
            ApiResponse(responseCode = "404", description = "사진을 찾을 수 없음 (PHOTO_001)", content = [Content()]),
        ],
    )
    fun delete(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "사진 ID", example = "1", required = true)
        id: Long,
    )
}
