package kr.co.lokit.api.domain.user.presentation

import jakarta.validation.Valid
import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.annotation.SyncCoupleStatusCookie
import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.common.dto.toIdResponse
import kr.co.lokit.api.domain.user.application.port.`in`.GetMyPageUseCase
import kr.co.lokit.api.domain.user.application.port.`in`.UpdateMyPageUseCase
import kr.co.lokit.api.domain.user.domain.User
import kr.co.lokit.api.domain.user.dto.MyPageResponse
import kr.co.lokit.api.domain.user.dto.UpdateNicknameRequest
import kr.co.lokit.api.domain.user.dto.UpdateProfileImageRequest
import kr.co.lokit.api.domain.user.presentation.mapping.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("my-page")
class MyPageController(
    private val getMyPageUseCase: GetMyPageUseCase,
    private val updateMyPageUseCase: UpdateMyPageUseCase,
) : MyPageApi {
    @SyncCoupleStatusCookie
    @GetMapping
    override fun getMyPage(
        @CurrentUserId userId: Long,
    ): MyPageResponse = getMyPageUseCase.getMyPage(userId).toResponse()

    @PutMapping("nickname")
    override fun updateNickname(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: UpdateNicknameRequest,
    ): IdResponse = updateMyPageUseCase.updateNickname(userId, request.nickname).toIdResponse(User::id)

    @PutMapping("profile-image")
    override fun updateProfileImage(
        @CurrentUserId userId: Long,
        @RequestBody @Valid request: UpdateProfileImageRequest,
    ): IdResponse = updateMyPageUseCase.updateProfileImage(userId, request.profileImageUrl).toIdResponse(User::id)
}
