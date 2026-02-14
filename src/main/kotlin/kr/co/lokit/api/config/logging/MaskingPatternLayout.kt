package kr.co.lokit.api.config.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import java.util.regex.Pattern

class MaskingPatternLayout : PatternLayout() {
    private val maskPatterns = mutableListOf<Pattern>()

    fun addMaskPattern(pattern: String) {
        maskPatterns.add(Pattern.compile(pattern.trim(), Pattern.CASE_INSENSITIVE or Pattern.MULTILINE))
    }

    override fun doLayout(event: ILoggingEvent): String {
        var message = super.doLayout(event)
        for (pattern in maskPatterns) {
            message = pattern.matcher(message).replaceAll("$1***")
        }
        return message
    }
}
