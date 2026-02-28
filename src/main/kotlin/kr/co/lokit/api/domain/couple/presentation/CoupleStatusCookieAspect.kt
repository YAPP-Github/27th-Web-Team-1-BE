package kr.co.lokit.api.domain.couple.presentation

import kr.co.lokit.api.config.security.UserPrincipal
import kr.co.lokit.api.config.web.CookieGenerator
import kr.co.lokit.api.common.annotation.SyncCoupleStatusCookie
import kr.co.lokit.api.domain.couple.application.CoupleCookieStatusResolver
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class CoupleStatusCookieAspect(
    private val coupleCookieStatusResolver: CoupleCookieStatusResolver,
    private val cookieGenerator: CookieGenerator,
) {
    @AfterReturning("@annotation(syncCoupleStatusCookie)")
    fun syncCoupleStatusCookie(
        @Suppress("UNUSED_PARAMETER") joinPoint: JoinPoint,
        @Suppress("UNUSED_PARAMETER") syncCoupleStatusCookie: SyncCoupleStatusCookie,
    ) {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return
        val request = attributes.request
        val response = attributes.response ?: return
        if (response.isCommitted) return

        val userId = resolveAuthenticatedUserId() ?: return
        val status = coupleCookieStatusResolver.resolve(userId)

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieGenerator.createCoupleStatusCookie(request, status).toString(),
        )
    }

    private fun resolveAuthenticatedUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated) return null
        return (authentication.principal as? UserPrincipal)?.id
    }
}
