package kr.co.lokit.api.common.constant

object GracePeriodPolicy {
    const val RECONNECT_DAYS = 31L
    const val PURGE_BUFFER_DAYS = 5L
    val PURGE_TOTAL_DAYS = RECONNECT_DAYS + PURGE_BUFFER_DAYS // 36
}
