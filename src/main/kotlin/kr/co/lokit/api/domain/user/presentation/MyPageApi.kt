package kr.co.lokit.api.domain.user.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.user.dto.MyPageResponse
import kr.co.lokit.api.domain.user.dto.UpdateNicknameRequest
import kr.co.lokit.api.domain.user.dto.UpdateProfileImageRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "MyPage", description = "마이페이지 & 온보딩 API")
interface MyPageApi {
    @Operation(
        summary = "마이페이지 조회",
        description = "내 정보와 상대방 정보, 커플 D+일수, 커플 전체 사진 수를 조회합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun getMyPage(
        @Parameter(hidden = true) userId: Long,
    ): MyPageResponse

    @Operation(
        summary = "닉네임 수정",
        description = "사용자의 닉네임을 수정합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun updateNickname(
        @Parameter(hidden = true) userId: Long,
        request: UpdateNicknameRequest,
    ): IdResponse

    @Operation(
        summary = "프로필 사진 수정",
        description = "사용자의 프로필 사진을 수정합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "프로필 사진 수정 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        ],
    )
    fun updateProfileImage(
        @Parameter(hidden = true) userId: Long,
        request: UpdateProfileImageRequest,
    ): IdResponse
}
