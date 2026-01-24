package kr.co.lokit.api.domain.workspace.presentation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.workspace.dto.CreateWorkspaceRequest
import kr.co.lokit.api.domain.workspace.dto.JoinWorkspaceRequest

@SecurityRequirement(name = "Authorization")
@Tag(name = "Workspace", description = "워크스페이스 API")
interface WorkspaceApi {

    @Operation(
        summary = "워크스페이스 생성",
        description = "새로운 워크스페이스를 생성합니다.",
        responses = [
            ApiResponse(responseCode = "201", description = "워크스페이스 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 입력값"),
        ],
    )
    fun create(request: CreateWorkspaceRequest, userId: Long): IdResponse

    @Operation(
        summary = "초대 코드로 워크스페이스 합류",
        description = "초대 코드를 통해 워크스페이스에 합류합니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "워크스페이스 합류 성공"),
            ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드"),
        ],
    )
    fun joinByInviteCode(request: JoinWorkspaceRequest, userId: Long): IdResponse
}
