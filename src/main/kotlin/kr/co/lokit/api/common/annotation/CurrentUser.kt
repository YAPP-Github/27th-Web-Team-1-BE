package kr.co.lokit.api.common.annotation

/**
 * 현재 인증된 사용자 정보를 DB에서 조회하여 가져오는 어노테이션
 * User 도메인 객체로 반환
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CurrentUser
