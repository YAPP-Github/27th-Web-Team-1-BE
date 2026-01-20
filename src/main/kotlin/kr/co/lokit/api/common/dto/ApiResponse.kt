package kr.co.lokit.api.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.servlet.http.HttpServletRequest
import kr.co.lokit.api.common.exception.ErrorCode
import org.springframework.http.HttpStatus

@Schema(description = "API 응답")
data class ApiResponse<T>(
    @Schema(description = "HTTP 상태 코드", example = "200")
    val code: Int,
    @Schema(description = "응답 메시지", example = "success")
    val message: String,
    @Schema(description = "응답 데이터")
    val data: T,
) {
    companion object {
        @Schema(description = "에러 상세 정보")
        data class ErrorDetail(
            @Schema(description = "에러 코드", example = "COMMON_001")
            val errorCode: String,
            @Schema(description = "에러 상세 메시지", example = "앨범을 찾을 수 없습니다.")
            val detail: String,
            @Schema(description = "요청 URI", example = "/api/albums/1")
            val instance: String,
            @Schema(description = "필드별 에러 목록", nullable = true)
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
