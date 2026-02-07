package kr.co.lokit.api.common.exception

sealed class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null,
    val errors: Map<String, String> = emptyMap(),
) : RuntimeException(message, cause) {

    class InvalidInputException(
        message: String = ErrorCode.INVALID_INPUT.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.INVALID_INPUT, message, cause, errors)

    class ResourceNotFoundException(
        message: String = ErrorCode.RESOURCE_NOT_FOUND.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.RESOURCE_NOT_FOUND, message, cause, errors)

    class ResourceAlreadyExistsException(
        message: String = ErrorCode.RESOURCE_ALREADY_EXISTS.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, message, cause, errors)

    class AuthenticationException(
        message: String = ErrorCode.UNAUTHORIZED.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.UNAUTHORIZED, message, cause, errors)

    class UnauthorizedException(
        message: String = ErrorCode.UNAUTHORIZED.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.UNAUTHORIZED, message, cause, errors)

    class ForbiddenException(
        message: String = ErrorCode.FORBIDDEN.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.FORBIDDEN, message, cause, errors)

    class BusinessRuleViolationException(
        message: String = ErrorCode.BUSINESS_RULE_VIOLATION.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message, cause, errors)

    class DefaultAlbumTitleChangeNotAllowedException(
        message: String = ErrorCode.ALBUM_DEFAULT_TITLE_CANNOT_CHANGE.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.ALBUM_DEFAULT_TITLE_CANNOT_CHANGE, message, cause, errors)

    class DefaultAlbumDeletionNotAllowedException(
        message: String = ErrorCode.ALBUM_DEFAULT_CANNOT_DELETE.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.ALBUM_DEFAULT_CANNOT_DELETE, message, cause, errors)

    class UserAlreadyExistsException(
        message: String = ErrorCode.EMAIL_ALREADY_EXISTS.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, message, cause, errors)

    class NotInitializedException(
        override val message: String,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.NOT_INITIALIZED_VALUE_ACCESS, message, errors = errors)

    class InvalidRefreshTokenException(
        message: String = ErrorCode.INVALID_REFRESH_TOKEN.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, message, cause, errors)

    class CoupleMaxMembersExceededException(
        message: String = ErrorCode.COUPLE_MAX_MEMBERS_EXCEEDED.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.COUPLE_MAX_MEMBERS_EXCEEDED, message, cause, errors)

    class UserNotFoundException(
        message: String = ErrorCode.USER_NOT_FOUND.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.USER_NOT_FOUND, message, cause, errors)

    class KakaoApiException(
        message: String = ErrorCode.KAKAO_API_ERROR.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.KAKAO_API_ERROR, message, cause, errors)

    class InvalidKakaoTokenException(
        message: String = ErrorCode.INVALID_KAKAO_TOKEN.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.INVALID_KAKAO_TOKEN, message, cause, errors)

    class KakaoEmailNotProvidedException(
        message: String = ErrorCode.KAKAO_EMAIL_NOT_PROVIDED.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.KAKAO_EMAIL_NOT_PROVIDED, message, cause, errors)

    // Photo
    class DefaultAlbumNotFoundForUserException(
        message: String = ErrorCode.DEFAULT_ALBUM_NOT_FOUND_FOR_USER.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.DEFAULT_ALBUM_NOT_FOUND_FOR_USER, message, cause, errors)

    class AlbumAlreadyExistsException(
        message: String = ErrorCode.ALBUM_ALREADY_EXISTS.message,
        cause: Throwable? = null,
        errors: Map<String, String> = emptyMap(),
    ) : BusinessException(ErrorCode.ALBUM_ALREADY_EXISTS, message, cause, errors)
}

inline fun <reified T> entityNotFound(id: Long): BusinessException.ResourceNotFoundException =
    BusinessException.ResourceNotFoundException(
        "${T::class.simpleName}(id=$id)을(를) 찾을 수 없습니다",
        errors = mapOf("id" to id.toString()),
    )

inline fun <reified T> entityNotFound(
    field: String,
    value: Any
): BusinessException.ResourceNotFoundException =
    BusinessException.ResourceNotFoundException(
        "${T::class.simpleName}을(를) ($value)로 찾을 수 없습니다",
        errors = mapOf(field to value.toString()),
    )

fun entityIdNotInitialized(entityName: String): BusinessException.NotInitializedException =
    BusinessException.NotInitializedException(
        "${entityName}의 id가 초기화되지 않았습니다",
        errors = mapOf("entityName" to entityName),
    )
