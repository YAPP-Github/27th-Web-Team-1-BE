package kr.co.lokit.api.config.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class LoginAccessDeniedHandler(
    @Qualifier("handlerExceptionResolver")
    private val resolver: HandlerExceptionResolver,
) : AccessDeniedHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        logger.error(
            "Access denied: method={}, uri={}, message={}",
            request.method,
            request.requestURI,
            accessDeniedException.message,
            accessDeniedException
        )
        response.status = HttpStatus.FORBIDDEN.value()
        resolver.resolveException(request, response, null, accessDeniedException)
    }
}
