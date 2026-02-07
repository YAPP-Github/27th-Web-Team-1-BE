package kr.co.lokit.api.domain.user.application.port

import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthUserInfo

interface OAuthClientPort {
    val provider: OAuthProvider

    fun getAccessToken(code: String): String

    fun getUserInfo(accessToken: String): OAuthUserInfo
}
