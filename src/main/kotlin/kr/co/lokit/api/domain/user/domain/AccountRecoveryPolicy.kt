package kr.co.lokit.api.domain.user.domain

import kr.co.lokit.api.common.constant.AccountStatus
import kr.co.lokit.api.common.constant.GracePeriodPolicy
import kr.co.lokit.api.common.exception.BusinessException
import kr.co.lokit.api.common.exception.ErrorField
import kr.co.lokit.api.common.exception.errorDetailsOf
import java.time.LocalDateTime

object AccountRecoveryPolicy {
    fun ensureRecoverable(
        user: User,
        now: LocalDateTime = LocalDateTime.now(),
    ): Boolean {
        if (user.status != AccountStatus.WITHDRAWN) {
            return false
        }
        val withdrawnAt =
            user.withdrawnAt
                ?: throw BusinessException.ForbiddenException(
                    message = "탈퇴 계정 정보가 유효하지 않습니다",
                    errors = errorDetailsOf(ErrorField.USER_ID to user.id),
                )
        if (withdrawnAt.plusDays(GracePeriodPolicy.RECONNECT_DAYS).isBefore(now)) {
            throw BusinessException.UserRecoveryExpiredException(
                errors =
                    errorDetailsOf(
                        ErrorField.USER_ID to user.id,
                        ErrorField.WITHDRAWN_AT to withdrawnAt,
                    ),
            )
        }
        return true
    }
}
