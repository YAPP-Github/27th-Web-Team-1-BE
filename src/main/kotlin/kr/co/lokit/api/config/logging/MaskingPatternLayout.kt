package kr.co.lokit.api.config.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent

class MaskingPatternLayout : PatternLayout() {
    private val engine = LogMaskingEngine.createDefault()

    override fun doLayout(event: ILoggingEvent): String {
        val message = super.doLayout(event)
        return engine.mask(message)
    }
}