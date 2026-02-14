package kr.co.lokit.api.common.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val key: String,
    val windowSeconds: Long,
    val maxRequests: Int,
)
