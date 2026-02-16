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
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "USER_001", "탈퇴한 사용자입니다"),
    USER_DISCONNECT_REQUIRED(HttpStatus.BAD_REQUEST, "USER_002", "회원 탈퇴 전 반드시 커플 연결 끊기를 완료해야 합니다"),
    USER_RECOVERY_EXPIRED(HttpStatus.GONE, "USER_003", "탈퇴 계정의 복구 가능 기간이 만료되었습니다"),

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

    // Comment
    COMMENT_MAX_EMOTICONS_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT_001", "댓글당 최대 10개의 이모지만 추가할 수 있습니다"),
    EMOTICON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_002", "이모지를 찾을 수 없습니다"),
    EMOTICON_ALREADY_EXISTS(HttpStatus.CONFLICT, "COMMENT_003", "이미 동일한 이모지를 추가했습니다"),

    // Couple
    COUPLE_MAX_MEMBERS_EXCEEDED(HttpStatus.BAD_REQUEST, "COUPLE_001", "커플 최대 인원을 초과했습니다"),
    COUPLE_ALREADY_CONNECTED(HttpStatus.CONFLICT, "COUPLE_002", "이미 다른 사용자와 커플로 연결되어 있습니다"),
    COUPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPLE_003", "커플을 찾을 수 없습니다"),
    COUPLE_ALREADY_DISCONNECTED(HttpStatus.CONFLICT, "COUPLE_004", "이미 연결이 해제된 커플입니다"),
    COUPLE_RECONNECT_EXPIRED(HttpStatus.GONE, "COUPLE_005", "재연결 가능 기간이 만료되었습니다"),
    COUPLE_NOT_DISCONNECTED(HttpStatus.BAD_REQUEST, "COUPLE_006", "연결 해제 상태가 아닌 커플입니다"),
    COUPLE_RECONNECT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "COUPLE_007", "해당 커플에 대한 재연결 권한이 없습니다"),
    INVITE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "COUPLE_008", "invalid_format"),
    INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPLE_009", "code_not_found"),
    INVITE_CODE_EXPIRED(HttpStatus.GONE, "COUPLE_010", "code_expired"),
    INVITE_CODE_USED(HttpStatus.CONFLICT, "COUPLE_011", "code_used"),
    INVITE_CODE_REVOKED(HttpStatus.CONFLICT, "COUPLE_012", "code_revoked"),
    INVITE_ALREADY_COUPLED(HttpStatus.CONFLICT, "COUPLE_013", "already_coupled"),
    INVITE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "COUPLE_014", "self_invite_not_allowed"),
    INVITE_NOT_OWNER(HttpStatus.FORBIDDEN, "COUPLE_015", "not_owner"),
    INVITE_ALREADY_USED(HttpStatus.CONFLICT, "COUPLE_016", "already_used"),
    INVITE_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COUPLE_017", "too_many_requests"),
    INVITE_RACE_CONFLICT(HttpStatus.CONFLICT, "COUPLE_018", "race_conflict"),
}
