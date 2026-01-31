package kr.co.lokit.api.config.security

import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CompositeAuthenticationResolver(
    resolvers: List<AuthenticationResolver>,
) {
    private val orderedResolvers =
        resolvers.sortedWith(AnnotationAwareOrderComparator.INSTANCE)

    fun authenticate(credentials: String): UsernamePasswordAuthenticationToken? =
        orderedResolvers.firstOrNull { it.support(credentials) }?.authenticate(credentials)
}
