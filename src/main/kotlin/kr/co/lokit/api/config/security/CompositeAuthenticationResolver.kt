package kr.co.lokit.api.config.security

import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationAwareOrderComparator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CompositeAuthenticationResolver(
    resolvers: List<AuthenticationResolver>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val orderedResolvers =
        resolvers.sortedWith(AnnotationAwareOrderComparator.INSTANCE)

    fun authenticate(credentials: String): UsernamePasswordAuthenticationToken? {
        val resolver = orderedResolvers.firstOrNull { it.support(credentials) }
        if (resolver == null) {
            logger.debug("No resolver supports the credentials")
            return null
        }
        logger.debug("Using resolver: {}", resolver.javaClass.simpleName)
        return resolver.authenticate(credentials)
    }
}
