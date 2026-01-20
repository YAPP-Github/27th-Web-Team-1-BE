package kr.co.lokit.api.domain.album.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "앨범 생성 요청")
data class AlbumRequest(
    @Schema(description = "앨범 제목", example = "여행 앨범")
    val title: String,
)
