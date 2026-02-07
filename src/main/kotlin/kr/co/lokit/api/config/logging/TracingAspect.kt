package kr.co.lokit.api.config.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Aspect
@Component
@Profile("local", "dev")
class TracingAspect {

    @Around(
        """
        (
            @within(org.springframework.stereotype.Service) ||
            @within(org.springframework.stereotype.Repository) ||
            @within(org.springframework.stereotype.Component)
        ) && within(kr.co.lokit.api.domain..*)
        """,
    )
    fun trace(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.nanoTime()
        try {
            return joinPoint.proceed()
        } finally {
            val durationMs = (System.nanoTime() - start) / 1_000_000
            val className = joinPoint.target.javaClass.simpleName
            val methodName = joinPoint.signature.name
            RequestTrace.add("$className.$methodName", durationMs)
        }
    }
}
