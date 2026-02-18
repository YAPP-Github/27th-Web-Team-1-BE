package kr.co.lokit.api.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class RefreshTokenHasher(
    @Value("\${jwt.secret}") private val secret: String,
) {
    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((secret + ":" + token).toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
