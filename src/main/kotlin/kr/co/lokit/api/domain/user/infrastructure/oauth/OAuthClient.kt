package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.domain.user.application.port.OAuthProvider
import kr.co.lokit.api.domain.user.application.port.OAuthUserInfo

interface OAuthClient {
    val provider: OAuthProvider

    fun getAccessToken(code: String): String

    fun getUserInfo(accessToken: String): OAuthUserInfo
}
