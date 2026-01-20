package kr.co.lokit.api.config.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.LayoutBase
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MaskingJsonLayout : LayoutBase<ILoggingEvent>() {
    private val objectMapper = JsonMapper.builder().build()
    private val dateFormatter =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.systemDefault())
    private val engine = LogMaskingEngine.createDefault()

    override fun doLayout(event: ILoggingEvent): String {
        val logMap = mutableMapOf<String, Any?>()

        logMap["timestamp"] = dateFormatter.format(Instant.ofEpochMilli(event.timeStamp))
        logMap["level"] = event.level.toString()
        logMap["logger"] = event.loggerName
        logMap["thread"] = event.threadName
        logMap["message"] = engine.mask(event.formattedMessage)

        // MDC properties
        val mdc = event.mdcPropertyMap
        if (mdc.isNotEmpty()) {
            val maskedMdc = mdc.mapValues { (_, value) -> engine.mask(value) }
            logMap["context"] = maskedMdc
        }

        // Exception
        if (event.throwableProxy != null) {
            logMap["exception"] = ThrowableProxyUtil.asString(event.throwableProxy)
        }

        return objectMapper.writeValueAsString(logMap) + "\n"
    }
}
