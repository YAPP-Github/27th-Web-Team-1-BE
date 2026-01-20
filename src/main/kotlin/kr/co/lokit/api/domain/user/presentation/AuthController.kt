package kr.co.lokit.api.domain.user.presentation

import kr.co.lokit.api.domain.user.application.AuthService
import kr.co.lokit.api.domain.user.dto.AuthResponse
import kr.co.lokit.api.domain.user.dto.RegisterRequest
import kr.co.lokit.api.domain.user.mapping.toAuthResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @RequestBody request: RegisterRequest,
    ): AuthResponse =
        authService
            .register(request.email, request.name)
            .toAuthResponse()
}
