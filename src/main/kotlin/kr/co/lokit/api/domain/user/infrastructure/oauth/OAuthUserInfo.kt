package kr.co.lokit.api.domain.user.infrastructure.oauth

interface OAuthUserInfo {
    val provider: OAuthProvider
    val providerId: String
    val email: String?
    val name: String?
    val profileImageUrl: String?
}
