package kr.co.lokit.api.common.dto

import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.exception.ErrorCode
import org.springframework.http.HttpStatus

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T,
) {
    companion object {
        data class ErrorDetail(
            val errorCode: String,
            val detail: String,
            val instance: String,
            val errors: Map<String, String>? = null,
        )

        fun <T> success(
            code: Int,
            data: T,
        ): ApiResponse<T> =
            ApiResponse(
                code = code,
                message = "success",
                data = data,
            )

        fun failure(
            exception: Exception,
            request: HttpServletRequest,
            errorCode: ErrorCode,
            errors: Map<String, String>? = null,
        ): ApiResponse<ErrorDetail> {
            val errorDetail =
                ErrorDetail(
                    errorCode = errorCode.code,
                    detail = exception.message ?: errorCode.message,
                    instance = request.requestURI,
                    errors = errors,
                )

            return ApiResponse(
                code = errorCode.status.value(),
                message = errorCode.status.reasonPhrase,
                data = errorDetail,
            )
        }

        fun failure(
            status: HttpStatus,
            detail: String,
            request: HttpServletRequest,
            errorCode: String,
            errors: Map<String, String>? = null,
        ): ApiResponse<ErrorDetail> {
            val errorDetail =
                ErrorDetail(
                    errorCode = errorCode,
                    detail = detail,
                    instance = request.requestURI,
                    errors = errors,
                )

            return ApiResponse(
                code = status.value(),
                message = status.reasonPhrase,
                data = errorDetail,
            )
        }
    }
}
