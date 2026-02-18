package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.domain.user.application.port.OAuthClientPort
import kr.co.lokit.api.domain.user.application.port.OAuthProvider
import kr.co.lokit.api.domain.user.application.port.OAuthUserInfo
import org.springframework.stereotype.Component

@Component
class KakaoOAuthClientAdapter(
    private val kakaoOAuthClient: KakaoOAuthClient,
) : OAuthClientPort {
    override val provider: OAuthProvider
        get() = kakaoOAuthClient.provider

    override fun getAccessToken(code: String): String = kakaoOAuthClient.getAccessToken(code)

    override fun getUserInfo(accessToken: String): OAuthUserInfo = kakaoOAuthClient.getUserInfo(accessToken)
}
