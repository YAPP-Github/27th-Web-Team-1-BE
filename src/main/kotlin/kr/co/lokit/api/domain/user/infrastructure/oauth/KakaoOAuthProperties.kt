package kr.co.lokit.api.domain.user.infrastructure.oauth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kakao")
data class KakaoOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val frontRedirectUri: String,
) {
    companion object {
        const val AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize"
        const val TOKEN_URL = "https://kauth.kakao.com/oauth/token"
        const val USER_INFO_URL = "https://kapi.kakao.com/v2/user/me"
    }
}
