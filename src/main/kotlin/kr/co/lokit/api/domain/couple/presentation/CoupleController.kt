package kr.co.lokit.api.domain.couple.presentation

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.annotation.SyncCoupleStatusCookie
import kr.co.lokit.api.domain.couple.application.CoupleCommandService
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest
import kr.co.lokit.api.domain.couple.dto.UpdateFirstMetDateRequest
import kr.co.lokit.api.domain.couple.dto.VerifyInviteCodeRequest
import kr.co.lokit.api.domain.couple.presentation.mapping.toResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("couples")
class CoupleController(
    private val disconnectCoupleUseCase: DisconnectCoupleUseCase,
    private val reconnectCoupleUseCase: ReconnectCoupleUseCase,
    private val coupleInviteUseCase: CoupleInviteUseCase,
    private val coupleCommandService: CoupleCommandService,
) : CoupleApi {
    @SyncCoupleStatusCookie
    @GetMapping("me/status")
    override fun getMyStatus(
        @CurrentUserId userId: Long,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ): CoupleStatusResponse {
        return coupleInviteUseCase.getMyStatus(userId).toResponse()
    }

    @SyncCoupleStatusCookie
    @PostMapping("me/cookie")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun saveCoupleStatusCookie(
        @CurrentUserId userId: Long,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ) = Unit

    @PostMapping("invites")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createInvite(
        @CurrentUserId userId: Long,
    ): InviteCodeResponse = coupleInviteUseCase.generateInviteCode(userId = userId).toResponse()

    @PostMapping("invites/refresh")
    override fun refreshInvite(
        @CurrentUserId userId: Long,
    ): InviteCodeResponse = coupleInviteUseCase.refreshInviteCode(userId = userId).toResponse()

    @PostMapping("invites/verify")
    override fun verifyInviteCode(
        @RequestBody @Valid request: VerifyInviteCodeRequest,
        @CurrentUserId userId: Long,
        httpRequest: HttpServletRequest,
    ): InviteCodePreviewResponse =
        coupleInviteUseCase
            .verifyInviteCode(
                userId = userId,
                inviteCode = request.inviteCode,
                clientIp = httpRequest.remoteAddr,
            ).toResponse()

    @SyncCoupleStatusCookie
    @PostMapping("join")
    override fun joinByInviteCode(
        @RequestBody @Valid request: JoinCoupleRequest,
        @CurrentUserId userId: Long,
        httpRequest: HttpServletRequest,
        res: HttpServletResponse,
    ): CoupleStatusResponse =
        coupleInviteUseCase
            .joinByInviteCode(
                userId = userId,
                inviteCode = request.inviteCode,
                clientIp = httpRequest.remoteAddr,
            ).toResponse()

    @SyncCoupleStatusCookie
    @PostMapping("reconnect")
    @ResponseStatus(HttpStatus.OK)
    override fun reconnect(
        @CurrentUserId userId: Long,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ): CoupleStatusResponse = reconnectCoupleUseCase.reconnect(userId).toResponse()

    @SyncCoupleStatusCookie
    @DeleteMapping("me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun disconnect(
        @CurrentUserId userId: Long,
        req: HttpServletRequest,
        res: HttpServletResponse,
    ) {
        disconnectCoupleUseCase.disconnect(userId)
    }

    @PatchMapping("me/first-met-date")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun updateFirstMetDate(
        @RequestBody @Valid request: UpdateFirstMetDateRequest,
        @CurrentUserId userId: Long,
    ) {
        coupleCommandService.updateFirstMetDate(userId, request.firstMetDate)
    }
}
