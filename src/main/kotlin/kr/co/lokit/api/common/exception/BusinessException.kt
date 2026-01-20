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
    ) : BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, message) {
        companion object {
            fun entityId() = NotInitializedException("Entity id is not initialized")
        }
    }
}
