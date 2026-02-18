package kr.co.lokit.api.domain.user.application.port

interface OAuthClientRegistryPort {
    fun getClient(provider: OAuthProvider): OAuthClientPort
}
