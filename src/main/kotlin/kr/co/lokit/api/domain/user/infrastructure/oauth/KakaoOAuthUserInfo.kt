package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.domain.user.application.port.OAuthProvider
import kr.co.lokit.api.domain.user.application.port.OAuthUserInfo
import kr.co.lokit.api.domain.user.dto.KakaoUserInfoResponse

class KakaoOAuthUserInfo(
    private val response: KakaoUserInfoResponse,
) : OAuthUserInfo {
    override val provider: OAuthProvider = OAuthProvider.KAKAO
    override val providerId: String = response.id.toString()
    override val email: String? = response.kakaoAccount?.email
}
