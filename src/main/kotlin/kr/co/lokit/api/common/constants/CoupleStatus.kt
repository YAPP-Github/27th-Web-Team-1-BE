package kr.co.lokit.api.common.constants

enum class CoupleStatus {
    CONNECTED,
    DISCONNECTED,
    EXPIRED,
    ;

    val isDisconnectedOrExpired: Boolean
        get() = this == DISCONNECTED || this == EXPIRED

    val selectionPriority: Int
        get() =
            when (this) {
                CONNECTED -> 0
                DISCONNECTED -> 1
                EXPIRED -> 2
            }
}
