package kr.co.lokit.api.domain.couple.domain

import java.security.SecureRandom

object InviteCodePolicy {
    const val EXPIRATION_HOURS = 24L
    const val RETRY_LIMIT = 5
    const val CODE_LENGTH = 6
    const val CODE_BOUND = 1_000_000
    private val CODE_FORMAT = Regex("^\\d{6}$")

    fun isValidFormat(code: String): Boolean = CODE_FORMAT.matches(code)

    fun generateCode(random: SecureRandom): String = random.nextInt(CODE_BOUND).toString().padStart(CODE_LENGTH, '0')
}
