package kr.co.lokit.api.domain.couple.presentation

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.couple.application.port.`in`.CoupleInviteUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.DisconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.ReconnectCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.dto.CoupleLinkResponse
import kr.co.lokit.api.domain.couple.dto.CoupleStatusResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodePreviewResponse
import kr.co.lokit.api.domain.couple.dto.InviteCodeResponse
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest
import kr.co.lokit.api.domain.couple.dto.VerifyInviteCodeRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("couples")
class CoupleController(
    private val createCoupleUseCase: CreateCoupleUseCase,
    private val disconnectCoupleUseCase: DisconnectCoupleUseCase,
    private val reconnectCoupleUseCase: ReconnectCoupleUseCase,
    private val coupleInviteUseCase: CoupleInviteUseCase,
) : CoupleApi {
    @GetMapping("code")
    override fun getCode(
        @CurrentUserId userId: Long,
    ): InviteCodeResponse = coupleInviteUseCase.generateInviteCode(userId = userId)

    @GetMapping("me/status")
    override fun getMyStatus(
        @CurrentUserId userId: Long,
    ): CoupleStatusResponse = coupleInviteUseCase.getMyStatus(userId)

    @PostMapping("invites")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createInvite(
        @CurrentUserId userId: Long,
    ): InviteCodeResponse = coupleInviteUseCase.generateInviteCode(userId = userId)

    @PostMapping("invites/refresh")
    override fun refreshInvite(
        @CurrentUserId userId: Long,
    ): InviteCodeResponse = coupleInviteUseCase.refreshInviteCode(userId = userId)

    @DeleteMapping("invites/{inviteCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun revokeInvite(
        @PathVariable inviteCode: String,
        @CurrentUserId userId: Long,
    ) {
        coupleInviteUseCase.revokeInviteCode(userId, inviteCode)
    }

    @PostMapping("invites/verify")
    override fun verifyInviteCode(
        @RequestBody @Valid request: VerifyInviteCodeRequest,
        @CurrentUserId userId: Long,
        httpRequest: HttpServletRequest,
    ): InviteCodePreviewResponse =
        coupleInviteUseCase.verifyInviteCode(
            userId = userId,
            inviteCode = request.inviteCode,
            clientIp = httpRequest.remoteAddr,
        )

    @PostMapping("invites/confirm")
    override fun confirmInviteCode(
        @RequestBody @Valid request: JoinCoupleRequest,
        @CurrentUserId userId: Long,
        httpRequest: HttpServletRequest,
    ): CoupleLinkResponse =
        coupleInviteUseCase.confirmInviteCode(
            userId = userId,
            inviteCode = request.inviteCode,
            clientIp = httpRequest.remoteAddr,
        )

    @PostMapping("join")
    override fun joinByInviteCode(
        @RequestBody @Valid request: JoinCoupleRequest,
        @CurrentUserId userId: Long,
        httpRequest: HttpServletRequest,
    ): IdResponse {
        val linked =
            coupleInviteUseCase.confirmInviteCode(
                userId = userId,
                inviteCode = request.inviteCode,
                clientIp = httpRequest.remoteAddr,
            )
        return IdResponse(linked.coupleId)
    }

    @PostMapping("reconnect")
    @ResponseStatus(HttpStatus.OK)
    override fun reconnect(
        @CurrentUserId userId: Long,
    ): IdResponse =
        reconnectCoupleUseCase
            .reconnect(userId)
            .toIdResponse(Couple::id)

    @DeleteMapping("me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun disconnect(
        @CurrentUserId userId: Long,
    ) {
        disconnectCoupleUseCase.disconnect(userId)
    }
}
