package kr.co.lokit.api.config.advice

import kr.co.lokit.api.common.dto.ApiResponse
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice(basePackages = ["kr.co.lokit.api"])
class ApiResponseAdvice : ResponseBodyAdvice<Any> {
    companion object {
        private val EXCLUDED_TYPES =
            setOf(
                ApiResponse::class.java,
                ResponseEntity::class.java,
                String::class.java,
                Void.TYPE,
                Unit::class.java,
            )
    }

    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean = returnType.parameterType !in EXCLUDED_TYPES

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        val statusCode =
            if (response is ServletServerHttpResponse) {
                response.servletResponse.status
            } else {
                HttpStatus.OK.value()
            }

        return ApiResponse.success(
            code = statusCode,
            data = body,
        )
    }
}
