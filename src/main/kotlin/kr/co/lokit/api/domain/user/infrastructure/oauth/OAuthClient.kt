package kr.co.lokit.api.domain.user.infrastructure.oauth

interface OAuthClient {
    val provider: OAuthProvider

    fun getAccessToken(code: String): String

    fun getUserInfo(accessToken: String): OAuthUserInfo
}
