package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.domain.user.application.port.OAuthClientPort
import org.springframework.stereotype.Component

@Component
class OAuthClientRegistry(
    clients: List<OAuthClientPort>,
) {
    private val clientMap: Map<OAuthProvider, OAuthClientPort> =
        clients.associateBy { it.provider }

    fun getClient(provider: OAuthProvider): OAuthClientPort =
        clientMap[provider]
            ?: throw IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: $provider")
}