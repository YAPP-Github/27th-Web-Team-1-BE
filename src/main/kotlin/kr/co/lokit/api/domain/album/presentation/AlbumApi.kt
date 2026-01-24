package kr.co.lokit.api.domain.album.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.album.dto.AlbumRequest
import kr.co.lokit.api.domain.album.dto.SelectableAlbumResponse
import kr.co.lokit.api.domain.album.dto.UpdateAlbumTitleRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Album", description = "앨범 API")
interface AlbumApi {

    @Operation(
        summary = "앨범 생성",
        description = "새로운 앨범을 생성합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "앨범 생성 성공"),
            ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음"),
        ],
    )
    fun create(albumRequest: AlbumRequest): IdResponse

    @Operation(
        summary = "선택 가능한 앨범 조회",
        description = "사용자가 선택할 수 있는 앨범 목록을 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "선택 가능한 앨범 조회 성공"),
        ],
    )
    fun getSelectableAlbums(userId: Long): SelectableAlbumResponse

    @Operation(
        summary = "앨범 제목 수정",
        description = "앨범의 제목을 수정합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "앨범 제목 수정 성공"),
            ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음"),
        ],
    )
    fun updateTitle(id: Long, request: UpdateAlbumTitleRequest): IdResponse

    @Operation(
        summary = "앨범 삭제",
        description = "앨범을 삭제합니다.",
        responses = [
            ApiResponse(responseCode = "204", description = "앨범 삭제 성공"),
            ApiResponse(responseCode = "404", description = "앨범을 찾을 수 없음"),
        ],
    )
    fun delete(id: Long)
}
