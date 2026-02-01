package kr.co.lokit.api.domain.album.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "앨범 생성 요청")
data class AlbumRequest(
    @field:NotBlank(message = "앨범 제목은 필수입니다.")
    @field:Size(max = 10, message = "앨범 제목은 10자 이내여야 합니다.")
    @Schema(description = "앨범 제목", example = "여행 앨범")
    val title: String,
)

@Schema(description = "앨범 제목 수정 요청")
data class UpdateAlbumTitleRequest(
    @field:Size(max = 10, message = "앨범 제목은 10자 이내여야 합니다.")
    @Schema(description = "앨범 제목", example = "새 앨범 이름")
    val title: String,
)

@Schema(description = "선택 가능한 앨범 응답")
data class SelectableAlbumResponse(
    @Schema(description = "선택 가능한 앨범 목록")
    val albums: List<SelectableAlbum>,
) {
    @Schema(description = "선택 가능한 앨범 정보")
    data class SelectableAlbum(
        @Schema(description = "앨범 ID", example = "1")
        val id: Long,
        @Schema(description = "앨범 제목", example = "여행 앨범")
        val title: String,
        @Schema(description = "앨범 내 사진 수", example = "10")
        val photoCount: Int,
        @Schema(description = "앨범 썸네일 URL", example = "https://example.com/thumbnail.jpg")
        val thumbnailUrl: String?,
    )
}
