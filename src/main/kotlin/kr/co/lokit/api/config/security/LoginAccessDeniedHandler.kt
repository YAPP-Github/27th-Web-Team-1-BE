package kr.co.lokit.api.config.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.common.dto.ApiResponse
import kr.co.lokit.api.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class LoginAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        logger.warn(
            "Access denied: method={}, uri={}, message={}",
            request.method,
            request.requestURI,
            accessDeniedException.message,
        )

        val errorCode = ErrorCode.FORBIDDEN
        val errorResponse =
            ApiResponse.failure(
                status = HttpStatus.FORBIDDEN,
                detail = accessDeniedException.message ?: errorCode.message,
                request = request,
                errorCode = errorCode.code,
            )

        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
