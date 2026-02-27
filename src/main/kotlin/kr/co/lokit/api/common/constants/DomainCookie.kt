package kr.co.lokit.api.common.constants

enum class DomainCookie(
    val value: String,
) {
    ACCESS_TOKEN("accessToken"),
    REFRESH_TOKEN("refreshToken"),
    COUPLE_STATUS("coupleStatus"),
}
