package kr.co.lokit.api.config.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.common.dto.ApiResponse
import kr.co.lokit.api.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class LoginAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.warn(
            "Authentication failed: method={}, uri={}, message={}",
            request.method,
            request.requestURI,
            authException.message
        )

        val errorCode = ErrorCode.UNAUTHORIZED
        val errorResponse = ApiResponse.failure(
            status = HttpStatus.UNAUTHORIZED,
            detail = authException.message ?: errorCode.message,
            request = request,
            errorCode = errorCode.code,
        )

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
