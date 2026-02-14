package kr.co.lokit.api.domain.user.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카카오 로그인 요청 (Authorization Code)")
data class KakaoLoginRequest(
    @Schema(description = "카카오 인가 코드", example = "authorization_code_from_kakao")
    val code: String,
)

data class KakaoTokenRequest(
    val grantType: String = "authorization_code",
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val code: String,
)

data class KakaoTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("expires_in")
    val expiresIn: Int,
    @JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Int?,
    val scope: String?,
)

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?,
)

data class KakaoAccount(
    val email: String?,
    val profile: KakaoProfile?,
    @JsonProperty("is_email_verified")
    val isEmailVerified: Boolean?,
)

data class KakaoProfile(
    val nickname: String?,
    @JsonProperty("profile_image_url")
    val profileImageUrl: String?,
)
