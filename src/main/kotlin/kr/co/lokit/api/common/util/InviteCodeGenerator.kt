package kr.co.lokit.api.common.util

import java.util.concurrent.ThreadLocalRandom

object InviteCodeGenerator {

    private const val CODE_LENGTH = 8

    fun generate(): String {
        return (1..CODE_LENGTH)
            .map { ThreadLocalRandom.current().nextInt(0, 10) }
            .joinToString()
    }
}
