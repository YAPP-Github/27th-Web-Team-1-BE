package kr.co.lokit.api.domain.user.presentation

import kr.co.lokit.api.common.dto.IdResponse
import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.application.TempLoginService
import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.dto.LoginRequest
import kr.co.lokit.api.domain.user.dto.RefreshTokenRequest
import kr.co.lokit.api.domain.user.mapping.toJwtTokenResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("auth")
class AuthController(
    private val authService: AuthService,
    private val tempLoginService: TempLoginService,
) : AuthApi {
    @PostMapping("login")
    @ResponseStatus(HttpStatus.OK)
    override fun login(
        @RequestBody request: LoginRequest,
    ): IdResponse =
        IdResponse(
            tempLoginService
                .login(request.email)
        )

    @PostMapping("refresh")
    override fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): JwtTokenResponse =
        authService
            .refresh(request.refreshToken)
            .toJwtTokenResponse()
}
