package kr.co.lokit.api.config.advice

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.lokit.api.common.dto.ApiResponse
import kr.co.lokit.api.common.dto.ApiResponse.Companion.ErrorDetail
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ErrorControllerAdvice {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ApiResponse<ErrorDetail> {
        response.status = ex.errorCode.status.value()

        return ApiResponse.failure(
            exception = ex,
            request = request,
            errorCode = ex.errorCode,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.BAD_REQUEST,
            detail = ErrorCode.INVALID_INPUT.message,
            request = request,
            errorCode = ErrorCode.INVALID_INPUT.code,
            errors =
                ex.bindingResult.fieldErrors.associate {
                    it.field to (it.defaultMessage ?: ex::class.java.name)
                },
        )

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> {
        val errors =
            ex.bindingResult.fieldErrors.associate {
                it.field to (it.defaultMessage ?: ex::class.java.name)
            }

        return ApiResponse.failure(
            status = HttpStatus.BAD_REQUEST,
            detail = ErrorCode.INVALID_INPUT.message,
            request = request,
            errorCode = ErrorCode.INVALID_INPUT.code,
            errors = errors,
        )
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        ex: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.BAD_REQUEST,
            detail = "${ex.parameterName} 파라미터가 필요합니다",
            request = request,
            errorCode = ErrorCode.MISSING_PARAMETER.code,
        )

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.BAD_REQUEST,
            detail = "${ex.name} 파라미터의 타입이 올바르지 않습니다",
            request = request,
            errorCode = ErrorCode.INVALID_TYPE.code,
        )

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.BAD_REQUEST,
            detail = "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요",
            request = request,
            errorCode = ErrorCode.INVALID_INPUT.code,
        )

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        ex: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            detail = "${ex.method} 메서드는 지원하지 않습니다",
            request = request,
            errorCode = ErrorCode.METHOD_NOT_ALLOWED.code,
        )

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.NOT_FOUND,
            detail = "요청한 리소스를 찾을 수 없습니다",
            request = request,
            errorCode = ErrorCode.RESOURCE_NOT_FOUND.code,
        )

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ApiResponse<ErrorDetail> =
        ApiResponse.failure(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            detail = ErrorCode.INTERNAL_SERVER_ERROR.message,
            request = request,
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR.code,
        )
}
