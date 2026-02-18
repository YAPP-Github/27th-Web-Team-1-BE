package kr.co.lokit.api.domain.user.application.port

interface OAuthClientPort {
    val provider: OAuthProvider

    fun getAccessToken(code: String): String

    fun getUserInfo(accessToken: String): OAuthUserInfo
}
