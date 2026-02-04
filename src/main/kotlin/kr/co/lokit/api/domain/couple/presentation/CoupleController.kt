package kr.co.lokit.api.domain.couple.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.couple.application.port.`in`.CreateCoupleUseCase
import kr.co.lokit.api.domain.couple.application.port.`in`.JoinCoupleUseCase
import kr.co.lokit.api.domain.couple.domain.Couple
import kr.co.lokit.api.domain.couple.dto.CreateCoupleRequest
import kr.co.lokit.api.domain.couple.dto.JoinCoupleRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("couples")
class CoupleController(
    private val createCoupleUseCase: CreateCoupleUseCase,
    private val joinCoupleUseCase: JoinCoupleUseCase,
) : CoupleApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @RequestBody @Valid request: CreateCoupleRequest,
        @CurrentUserId userId: Long,
    ): IdResponse =
        createCoupleUseCase.createIfNone(Couple(name = request.name), userId)
            .toIdResponse(Couple::id)

    @PostMapping("join")
    @ResponseStatus(HttpStatus.OK)
    override fun joinByInviteCode(
        @RequestBody @Valid request: JoinCoupleRequest,
        @CurrentUserId userId: Long,
    ): IdResponse =
        joinCoupleUseCase.joinByInviteCode(request.inviteCode, userId)
            .toIdResponse(Couple::id)
}
