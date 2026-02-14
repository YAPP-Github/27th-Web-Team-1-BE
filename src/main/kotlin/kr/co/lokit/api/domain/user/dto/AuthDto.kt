package kr.co.lokit.api.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로그인 요청")
data class LoginRequest(
    @Schema(description = "사용자 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    val email: String,
)

@Schema(description = "JWT 토큰 응답")
data class JwtTokenResponse(
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    val refreshToken: String,
)
