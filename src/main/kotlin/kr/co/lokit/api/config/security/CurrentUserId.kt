package kr.co.lokit.api.config.security

/**
 * 현재 인증된 사용자의 ID만 가져오는 어노테이션
 * DB 조회 없이 SecurityContext에서 직접 추출
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CurrentUserId