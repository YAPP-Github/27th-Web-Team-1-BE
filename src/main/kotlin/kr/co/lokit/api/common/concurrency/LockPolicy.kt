package kr.co.lokit.api.common.concurrency

object LockPolicy {
    const val JVM_LOCK_TIMEOUT_SECONDS: Long = 10
    const val DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS: Int = 3000
    const val DB_PESSIMISTIC_LOCK_TIMEOUT_MILLIS_TEXT: String = "3000"
}
