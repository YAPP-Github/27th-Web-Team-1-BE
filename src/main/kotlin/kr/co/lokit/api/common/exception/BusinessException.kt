package kr.co.lokit.api.common.exception

sealed class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class InvalidInputException(
        message: String = ErrorCode.INVALID_INPUT.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.INVALID_INPUT, message, cause)

    class ResourceNotFoundException(
        message: String = ErrorCode.RESOURCE_NOT_FOUND.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message, cause)

    class ResourceAlreadyExistsException(
        message: String = ErrorCode.RESOURCE_ALREADY_EXISTS.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, message, cause)

    class UnauthorizedException(
        message: String = ErrorCode.UNAUTHORIZED.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.UNAUTHORIZED, message, cause)

    class ForbiddenException(
        message: String = ErrorCode.FORBIDDEN.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.FORBIDDEN, message, cause)

    class BusinessRuleViolationException(
        message: String = ErrorCode.BUSINESS_RULE_VIOLATION.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message, cause)

    class UserAlreadyExistsException(
        message: String = ErrorCode.EMAIL_ALREADY_EXISTS.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, message, cause)

    class NotInitializedException(
        override val message: String,
    ) : BusinessException(ErrorCode.NOT_INITIALIZED_VALUE_ACCESS, message)

    class InvalidRefreshTokenException(
        message: String = ErrorCode.INVALID_REFRESH_TOKEN.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, message, cause)

    class UserNotFoundException(
        message: String = ErrorCode.USER_NOT_FOUND.message,
        cause: Throwable? = null,
    ) : BusinessException(ErrorCode.USER_NOT_FOUND, message, cause)
}

inline fun <reified T> entityNotFound(id: Long): BusinessException.ResourceNotFoundException =
    BusinessException.ResourceNotFoundException("${T::class.simpleName}(id=$id)을(를) 찾을 수 없습니다")

fun entityIdNotInitialized(entityName: String): BusinessException.NotInitializedException =
    BusinessException.NotInitializedException("${entityName}의 id가 초기화되지 않았습니다")
