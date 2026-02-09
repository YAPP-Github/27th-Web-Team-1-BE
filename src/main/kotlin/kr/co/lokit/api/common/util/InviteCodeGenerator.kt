package kr.co.lokit.api.common.util

import kr.co.lokit.api.domain.couple.domain.Couple.Companion.INVITE_CODE_LENGTH
import java.util.concurrent.ThreadLocalRandom

object InviteCodeGenerator {
    private const val DIGIT_BOUND = 10

    fun generate(): String =
        buildString(INVITE_CODE_LENGTH) {
            repeat(INVITE_CODE_LENGTH) {
                append(ThreadLocalRandom.current().nextInt(DIGIT_BOUND))
            }
        }
}
