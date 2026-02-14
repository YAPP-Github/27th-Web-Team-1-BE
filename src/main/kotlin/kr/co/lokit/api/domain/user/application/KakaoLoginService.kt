package kr.co.lokit.api.domain.user.application

import kr.co.lokit.api.domain.user.dto.JwtTokenResponse
import kr.co.lokit.api.domain.user.infrastructure.oauth.OAuthProvider
import org.springframework.stereotype.Service

@Service
class KakaoLoginService(
    private val oAuthService: OAuthService,
) {
    fun login(code: String): JwtTokenResponse = oAuthService.login(OAuthProvider.KAKAO, code)
}
