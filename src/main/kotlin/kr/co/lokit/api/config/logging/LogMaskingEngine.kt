package kr.co.lokit.api.config.logging

import java.util.regex.Pattern

class LogMaskingEngine private constructor(
    patterns: List<String>,
) {
    // 1. 모든 패턴을 하나로 합침 ((?i)는 대소문자 무시라는 뜻이야)
    private val combinedPattern: Pattern =
        Pattern.compile(
            patterns.joinToString("|"),
            Pattern.CASE_INSENSITIVE or Pattern.MULTILINE,
        )

    fun mask(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val matcher = combinedPattern.matcher(input)
        val sb = StringBuilder(input.length)
        var lastEnd = 0

        while (matcher.find()) {
            sb.append(input, lastEnd, matcher.start())
            // 2. 매칭된 텍스트를 통째로 가져와서 마스킹 로직에 던짐
            sb.append(processMasking(matcher.group()))
            lastEnd = matcher.end()
        }
        sb.append(input, lastEnd, input.length)
        return sb.toString()
    }

    private fun processMasking(match: String): String =
        try {
            // 키워드(password, email 등)가 포함되어 있는지 확인
            when {
                // [A] 비밀번호 및 토큰 (전체 마스킹)
                match.contains("password", true) || match.contains("pwd", true) ||
                    match.contains("token", true) || match.contains("secret", true) -> {
                    // 기호(: 또는 =)를 기준으로 앞부분(키)만 살리고 뒷부분은 ***
                    val delimiter = if (match.contains(":")) ":" else "="
                    val key = match.substringBefore(delimiter)
                    "$key$delimiter ***"
                }

                // [B] 이메일 (앞 2글자 유지)
                match.contains("email", true) -> {
                    val delimiter = if (match.contains(":")) ":" else "="
                    val key = match.substringBefore(delimiter)
                    val value = match.substringAfter(delimiter).replace("\"", "").trim()
                    "$key$delimiter \"${value.take(2)}***@${value.substringAfter("@")}\""
                }

                // [C] 전화번호/카드번호 (뒤 4자리 유지)
                match.contains("phone", true) || match.contains("cardNumber", true) -> {
                    val delimiter = if (match.contains(":")) ":" else "="
                    val key = match.substringBefore(delimiter)
                    val value = match.substringAfter(delimiter).replace("\"", "").trim()
                    val last4 = value.takeLast(4)
                    val masked = if (match.contains("cardNumber", true)) "****-****-****-$last4" else "***$last4"
                    "$key$delimiter \"$masked\""
                }

                else -> {
                    "***"
                }
            }
        } catch (e: Exception) {
            "***" // 에러 나면 안전하게 가리기
        }

    companion object {
        // [핵심] JSON 형태와 일반 텍스트 형태를 모두 잡는 정규식 세트
        private val SENSITIVE_PATTERNS =
            listOf(
                // 1. 일반 텍스트용: password : 1234 또는 password=1234
                """(?i)(password|pwd|token|secret|accessToken|refreshToken)\s*[:=]\s*\S+""",
                // 2. JSON 형태용: "password":"1234"
                """"(password|pwd|token|secret|accessToken|refreshToken)"\s*[:=]\s*"[^"]*"""",
                // 3. 이메일, 전화번호, 카드번호용
                """"email"\s*[:=]\s*"[^"]+@[^"]+"""",
                """(?i)email\s*[:=]\s*\S+@\S+""",
                """"(phone|cardNumber)"\s*[:=]\s*"[^"]*"""",
            )

        fun createDefault() = LogMaskingEngine(SENSITIVE_PATTERNS)
    }
}
