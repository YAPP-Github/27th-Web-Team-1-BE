package kr.co.lokit.api.domain.couple.domain

object InviteVerificationPolicy {
    const val FAILURE_WINDOW_SECONDS = 60L
    const val MAX_FAILURES = 5
    const val COOLDOWN_SECONDS = 60L
}
