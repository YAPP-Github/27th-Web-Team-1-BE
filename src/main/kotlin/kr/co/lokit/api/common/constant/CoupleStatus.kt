package kr.co.lokit.api.common.constant

enum class CoupleStatus {
    CONNECTED,
    DISCONNECTED,
    EXPIRED,
    ;

    val isDisconnectedOrExpired: Boolean
        get() = this == DISCONNECTED || this == EXPIRED
}
