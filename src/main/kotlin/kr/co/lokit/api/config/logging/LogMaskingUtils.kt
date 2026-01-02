package kr.co.lokit.api.config.logging

object LogMaskingUtils {
    private val SENSITIVE_PATTERNS = listOf(
        // password variants
        """"password"\s*:\s*"[^"]*"""" to """"password":"***"""",
        """"pwd"\s*:\s*"[^"]*"""" to """"pwd":"***"""",
        // email - show first 2 chars
        """"email"\s*:\s*"([^"]{2})[^"]*@([^"]+)"""" to """"email":"$1***@$2"""",
        // phone number - show last 4 digits
        """"phone"\s*:\s*"[^"]*([0-9]{4})"""" to """"phone":"***$1"""",
        """"phoneNumber"\s*:\s*"[^"]*([0-9]{4})"""" to """"phoneNumber":"***$1"""",
        // credit card - show last 4 digits
        """"cardNumber"\s*:\s*"[^"]*([0-9]{4})"""" to """"cardNumber":"****-****-****-$1"""",
        // token/secret
        """"token"\s*:\s*"[^"]*"""" to """"token":"***"""",
        """"secret"\s*:\s*"[^"]*"""" to """"secret":"***"""",
        """"accessToken"\s*:\s*"[^"]*"""" to """"accessToken":"***"""",
        """"refreshToken"\s*:\s*"[^"]*"""" to """"refreshToken":"***"""",
    )

    fun mask(content: String): String {
        var masked = content
        SENSITIVE_PATTERNS.forEach { (pattern, replacement) ->
            masked = masked.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        }
        return masked
    }
}
