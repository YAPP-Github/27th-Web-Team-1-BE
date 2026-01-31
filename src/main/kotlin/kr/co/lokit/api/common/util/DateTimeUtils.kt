package kr.co.lokit.api.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")

    fun LocalDateTime.toDateString(): String =
        this.atZone(ZONE_ID).format(DATE_FORMATTER)

    fun LocalDateTime.toDateTimeString(): String =
        this.atZone(ZONE_ID).format(DATE_TIME_FORMATTER)
}
