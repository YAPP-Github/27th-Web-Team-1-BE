package kr.co.lokit.api.config.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.LayoutBase
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class MaskingJsonLayout : LayoutBase<ILoggingEvent>() {
    private val objectMapper = JsonMapper.builder().build()
    var prettyPrint: Boolean = false
    private val dateFormatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.systemDefault())
    private val maskPatterns = mutableListOf<Pattern>()

    fun addMaskPattern(pattern: String) {
        maskPatterns.add(Pattern.compile(pattern.trim(), Pattern.CASE_INSENSITIVE or Pattern.MULTILINE))
    }

    private fun mask(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        var result = input
        for (pattern in maskPatterns) {
            result = pattern.matcher(result).replaceAll("$1***")
        }
        return result
    }

    override fun doLayout(event: ILoggingEvent): String {
        val logMap = mutableMapOf<String, Any?>()

        logMap["timestamp"] = dateFormatter.format(Instant.ofEpochMilli(event.timeStamp))
        logMap["level"] = event.level.toString()
        logMap["logger"] = event.loggerName
        logMap["thread"] = event.threadName
        logMap["message"] = mask(event.formattedMessage)

        // MDC properties
        val mdc = event.mdcPropertyMap
        if (mdc.isNotEmpty()) {
            val maskedMdc = mdc.mapValues { (_, value) -> mask(value) }
            logMap["context"] = maskedMdc
        }

        // Exception
        if (event.throwableProxy != null) {
            logMap["exception"] = ThrowableProxyUtil.asString(event.throwableProxy)
        }

        val json =
            if (prettyPrint) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logMap)
            } else {
                objectMapper.writeValueAsString(logMap)
            }
        return json + "\n"
    }
}
