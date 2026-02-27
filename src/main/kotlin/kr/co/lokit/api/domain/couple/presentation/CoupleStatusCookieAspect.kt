package kr.co.lokit.api.domain.couple.presentation

import kr.co.lokit.api.common.annotation.CurrentUserId
import kr.co.lokit.api.common.annotation.SyncCoupleStatusCookie
import kr.co.lokit.api.config.cache.CacheRegion
import kr.co.lokit.api.config.cache.evictKey
import kr.co.lokit.api.config.cache.evictUserCoupleCache
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.domain.couple.application.CoupleCookieStatusResolver
import kr.co.lokit.api.domain.couple.application.port.CoupleRepositoryPort
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.cache.CacheManager
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Method

@Aspect
@Component
class CoupleStatusCookieAspect(
    private val coupleCookieStatusResolver: CoupleCookieStatusResolver,
    private val coupleRepository: CoupleRepositoryPort,
    private val cookieGenerator: CookieGenerator,
    private val cacheManager: CacheManager,
) {
    @AfterReturning("@annotation(syncCoupleStatusCookie)")
    fun syncCoupleStatusCookie(
        joinPoint: JoinPoint,
        @Suppress("UNUSED_PARAMETER") syncCoupleStatusCookie: SyncCoupleStatusCookie,
    ) {
        val userId = resolveCurrentUserId(joinPoint) ?: return
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return
        val request = attributes.request
        val response = attributes.response ?: return

        evictCoupleCachesForCookie(userId)

        val coupleStatus = coupleCookieStatusResolver.resolve(userId)
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        response.setHeader("Pragma", "no-cache")
        response.setDateHeader("Expires", 0)
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieGenerator.clearCoupleStatusCookie(request).toString(),
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieGenerator.createCoupleStatusCookie(request, coupleStatus).toString(),
        )
    }

    private fun resolveCurrentUserId(joinPoint: JoinPoint): Long? {
        val methodSignature = joinPoint.signature as? MethodSignature ?: return null
        val interfaceMethod = methodSignature.method
        val targetClass = joinPoint.target?.javaClass
        val targetMethod =
            if (targetClass != null) {
                AopUtils.getMostSpecificMethod(interfaceMethod, targetClass)
            } else {
                interfaceMethod
            }

        return resolveCurrentUserIdByAnnotation(targetMethod, joinPoint.args)
            ?: resolveCurrentUserIdByAnnotation(interfaceMethod, joinPoint.args)
            ?: (joinPoint.args.firstOrNull { it is Long } as? Long)
    }

    private fun resolveCurrentUserIdByAnnotation(
        method: Method,
        args: Array<Any?>,
    ): Long? {
        method.parameters.forEachIndexed { index, parameter ->
            if (parameter.getAnnotation(CurrentUserId::class.java) != null) {
                return args.getOrNull(index) as? Long
            }
        }
        return null
    }

    private fun evictCoupleCachesForCookie(userId: Long) {
        cacheManager.evictUserCoupleCache(userId)

        val currentCoupleId = coupleRepository.findByUserIdFresh(userId)?.id
        if (currentCoupleId != null) {
            cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, currentCoupleId)
        }

        val disconnectedCoupleId = coupleRepository.findByDisconnectedByUserId(userId)?.id
        if (disconnectedCoupleId != null && disconnectedCoupleId != currentCoupleId) {
            cacheManager.evictKey(CacheRegion.COUPLE_ALBUMS, disconnectedCoupleId)
        }
    }
}
