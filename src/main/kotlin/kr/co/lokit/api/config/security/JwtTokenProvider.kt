package kr.co.lokit.api.config.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.co.lokit.api.domain.user.domain.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
@Profile("!local", "!dev")
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
    @Value("\${spring.profiles.active}") private val profile: String
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userDetails: UserDetails): String = generateAccessToken(userDetails.username)

    fun generateAccessToken(user: User): String = generateAccessToken(user.email)

    private fun generateAccessToken(subject: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts
            .builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun getRefreshTokenExpirationMillis(): Long = refreshExpiration

    fun getUsernameFromToken(token: String): String = getClaims(token).subject

    fun validateToken(
        token: String,
        userDetails: UserDetails,
    ): Boolean {
        val username = getUsernameFromToken(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    fun canParse(token: String): Boolean =
        profile != "local" && (token.startsWith("bearer") || token.startsWith("Bearer"))

    private fun isTokenExpired(token: String): Boolean = getClaims(token).expiration.before(Date())

    private fun getClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
