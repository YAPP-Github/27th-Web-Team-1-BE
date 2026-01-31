package kr.co.lokit.api.domain.workspace.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.workspace.application.WorkspaceService
import kr.co.lokit.api.domain.workspace.domain.Workspace
import kr.co.lokit.api.domain.workspace.dto.CreateWorkspaceRequest
import kr.co.lokit.api.domain.workspace.dto.JoinWorkspaceRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("workspaces")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
) : WorkspaceApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody @Valid request: CreateWorkspaceRequest,
        @CurrentUserId userId: Long,
    ): IdResponse =
        workspaceService.createIfNone(Workspace(name = request.name), userId)
            .toIdResponse(Workspace::id)

    @PostMapping("join")
    @ResponseStatus(HttpStatus.OK)
    override fun joinByInviteCode(
        @RequestBody @Valid request: JoinWorkspaceRequest,
        @CurrentUserId userId: Long,
    ): IdResponse =
        workspaceService.joinByInviteCode(request.inviteCode, userId)
            .toIdResponse(Workspace::id)
}
