package kr.co.lokit.api.domain.album.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max

@Schema(description = "앨범 생성 요청")
data class AlbumRequest(
    @field:Max(10, message = "앨범 제목은 10자 이내여야 합니다.")
    @Schema(description = "앨범 제목", example = "여행 앨범")
    val title: String,
    @Schema(description = "워크스페이스 ID", example = "1")
    val workspaceId: Long,
)

data class SelectableAlbumResponse(
    val albums: List<SelectableAlbum>,
) {
    data class SelectableAlbum(
        val id: Long,
        val title: String,
        val photoCount: Int,
        val thumbnailUrl: String?,
    )
}
