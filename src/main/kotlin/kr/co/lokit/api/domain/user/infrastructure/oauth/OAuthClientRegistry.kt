package kr.co.lokit.api.domain.user.infrastructure.oauth

import kr.co.lokit.api.domain.user.application.port.OAuthClientPort
import kr.co.lokit.api.domain.user.application.port.OAuthClientRegistryPort
import kr.co.lokit.api.domain.user.application.port.OAuthProvider
import org.springframework.stereotype.Component

@Component
class OAuthClientRegistry(
    clients: List<OAuthClientPort>,
) : OAuthClientRegistryPort {
    private val clientMap: Map<OAuthProvider, OAuthClientPort> =
        clients.associateBy { it.provider }

    override fun getClient(provider: OAuthProvider): OAuthClientPort =
        clientMap[provider]
            ?: throw IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: $provider")
}
