package kr.co.lokit.api.common.ratelimit

import kr.co.lokit.api.common.annotation.RateLimit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.stereotype.Component
import org.springframework.expression.spel.standard.SpelExpressionParser

@Aspect
@Component
class RateLimitAspect(
    private val inMemoryRateLimiter: InMemoryRateLimiter,
) {
    private val expressionParser = SpelExpressionParser()
    private val parameterNameDiscoverer: ParameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(rateLimit)")
    fun applyRateLimit(
        joinPoint: ProceedingJoinPoint,
        rateLimit: RateLimit,
    ): Any? {
        val signature = joinPoint.signature as MethodSignature
        val evaluationContext =
            MethodBasedEvaluationContext(
                joinPoint.target,
                signature.method,
                joinPoint.args,
                parameterNameDiscoverer,
            )

        val key =
            expressionParser
                .parseExpression(rateLimit.key)
                .getValue(evaluationContext, String::class.java)
                ?: throw IllegalArgumentException("Rate limit key must resolve to non-null String.")

        inMemoryRateLimiter.checkAllowed(
            key = key,
            windowSeconds = rateLimit.windowSeconds,
            maxRequests = rateLimit.maxRequests,
        )
        return joinPoint.proceed()
    }
}
