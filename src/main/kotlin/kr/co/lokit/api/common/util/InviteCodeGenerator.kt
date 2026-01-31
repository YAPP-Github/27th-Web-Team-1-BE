package kr.co.lokit.api.common.util

import java.util.concurrent.ThreadLocalRandom

object InviteCodeGenerator {

    private const val CODE_LENGTH = 8
    private const val DIGIT_BOUND = 10

    fun generate(): String {
        return buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) {
                append(ThreadLocalRandom.current().nextInt(DIGIT_BOUND))
            }
        }
    }
}
