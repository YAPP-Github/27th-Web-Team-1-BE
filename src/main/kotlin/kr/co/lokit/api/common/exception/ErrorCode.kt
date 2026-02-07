package kr.co.lokit.api.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다"),
    INVALID_TYPE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 타입입니다"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON_003", "필수 파라미터가 누락되었습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_004", "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_005", "서버 내부 오류가 발생했습니다"),
    NOT_INITIALIZED_VALUE_ACCESS(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_006", "초기화되지 않은 값을 접근했습니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_005", "이미 등록된 이메일입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "유효하지 않은 리프레시 토큰입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_007", "사용자를 찾을 수 없습니다"),

    // Kakao OAuth
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_001", "카카오 API 호출에 실패했습니다"),
    INVALID_KAKAO_TOKEN(HttpStatus.UNAUTHORIZED, "KAKAO_002", "유효하지 않은 카카오 액세스 토큰입니다"),
    KAKAO_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "KAKAO_003", "카카오 계정에서 이메일 정보를 제공받지 못했습니다"),

    // Resource
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "RESOURCE_002", "이미 존재하는 리소스입니다"),

    // Concurrency
    CONFLICT(HttpStatus.CONFLICT, "COMMON_007", "동시 수정 충돌이 발생했습니다. 다시 시도해주세요"),

    // Business
    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "BUSINESS_001", "비즈니스 규칙 위반입니다"),

    // Album
    ALBUM_DEFAULT_TITLE_CANNOT_CHANGE(HttpStatus.BAD_REQUEST, "ALBUM_001", "기본 앨범의 이름은 변경할 수 없습니다"),
    ALBUM_DEFAULT_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "ALBUM_002", "기본 앨범은 삭제할 수 없습니다"),
    ALBUM_ALREADY_EXISTS(HttpStatus.CONFLICT, "ALBUM_003", "동일한 이름의 앨범이 이미 존재합니다"),

    // Photo
    DEFAULT_ALBUM_NOT_FOUND_FOR_USER(HttpStatus.INTERNAL_SERVER_ERROR, "PHOTO_001", "사용자의 기본 앨범을 찾을 수 없습니다"),

    // Couple
    COUPLE_MAX_MEMBERS_EXCEEDED(HttpStatus.BAD_REQUEST, "COUPLE_001", "커플 최대 인원을 초과했습니다"),
}
