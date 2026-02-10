package kr.co.lokit.api.config.notification

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PreDestroy
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(name = ["discord.webhook.url"])
@Profile("!local")
class DiscordNotifier(
    @Value("\${discord.webhook.url}") private val webhookUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder()
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5_000)
            setReadTimeout(10_000)
        })
        .build()

    private val backoffUntil =
        Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(200)
            .build<String, Instant>()

    private val backoffStep =
        Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(200)
            .build<String, Int>()

    private val pendingErrors = ConcurrentLinkedQueue<ErrorSnapshot>()
    private val flushScheduled = AtomicBoolean(false)

    companion object {
        private const val STACKTRACE_MAX_LENGTH = 800
        private const val BATCH_WINDOW_SECONDS = 10L
        private const val MAX_EMBEDS_PER_MESSAGE = 10
        private val BACKOFF_DURATIONS = listOf(
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1),
        )
        private val KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Seoul"))
    }

    @PreDestroy
    fun notifyShutdown() {
        try {
            val timestamp = KST_FORMATTER.format(Instant.now())
            val payload = mapOf(
                "embeds" to listOf(
                    mapOf(
                        "title" to "서버 종료",
                        "color" to 0xFFA500,
                        "fields" to listOf(
                            mapOf("name" to "Timestamp", "value" to timestamp, "inline" to false),
                        ),
                    ),
                ),
            )
            restClient.post()
                .uri(webhookUrl)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            log.warn("Discord 종료 알림 전송 실패: ${e.message}")
        }
    }

    @Async
    @EventListener(ApplicationReadyEvent::class)
    fun notifyDeployment() {
        try {
            val timestamp = KST_FORMATTER.format(Instant.now())
            val payload = mapOf(
                "embeds" to listOf(
                    mapOf(
                        "title" to "서버 배포 완료",
                        "color" to 0x00FF00,
                        "fields" to listOf(
                            mapOf("name" to "Timestamp", "value" to timestamp, "inline" to false),
                        ),
                    ),
                ),
            )
            restClient.post()
                .uri(webhookUrl)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            log.warn("Discord 배포 알림 전송 실패: ${e.message}")
        }
    }

    fun notify(ex: Exception, request: HttpServletRequest, trace: String? = null) {
        val deduplicationKey = "${ex::class.simpleName}:${ex.message.hashCode()}"
        val now = Instant.now()

        val until = backoffUntil.getIfPresent(deduplicationKey)
        if (until != null && now.isBefore(until)) return

        val step = (backoffStep.getIfPresent(deduplicationKey) ?: -1) + 1
        val cappedStep = step.coerceAtMost(BACKOFF_DURATIONS.size - 1)
        backoffStep.put(deduplicationKey, cappedStep)
        backoffUntil.put(deduplicationKey, now.plus(BACKOFF_DURATIONS[cappedStep]))

        pendingErrors.add(
            ErrorSnapshot(
                exceptionClass = ex::class.simpleName ?: "Unknown",
                message = ex.message ?: "No message",
                method = request.method,
                uri = request.requestURI,
                stacktrace = ex.stackTraceToString().take(STACKTRACE_MAX_LENGTH),
                timestamp = KST_FORMATTER.format(now),
                trace = trace,
            ),
        )

        if (flushScheduled.compareAndSet(false, true)) {
            Thread.startVirtualThread {
                try {
                    Thread.sleep(Duration.ofSeconds(BATCH_WINDOW_SECONDS))
                    flush()
                } catch (e: Exception) {
                    log.warn("Discord 알림 전송 실패: ${e.message}")
                } finally {
                    flushScheduled.set(false)
                }
            }
        }
    }

    private fun flush() {
        val batch = mutableListOf<ErrorSnapshot>()
        while (batch.size < MAX_EMBEDS_PER_MESSAGE) {
            batch.add(pendingErrors.poll() ?: break)
        }
        if (batch.isEmpty()) return

        pendingErrors.clear()

        val embeds = batch.map { it.toEmbed() }
        val payload = mapOf("embeds" to embeds)

        restClient.post()
            .uri(webhookUrl)
            .header("Content-Type", "application/json")
            .body(payload)
            .retrieve()
            .toBodilessEntity()
    }

    private data class ErrorSnapshot(
        val exceptionClass: String,
        val message: String,
        val method: String,
        val uri: String,
        val stacktrace: String,
        val timestamp: String,
        val trace: String? = null,
    ) {
        fun toEmbed(): Map<String, Any> {
            val fields = mutableListOf(
                field("Message", message),
                field("Request", "$method $uri"),
                field("Timestamp", timestamp),
                field("Stacktrace", "```\n$stacktrace\n```"),
            )
            if (!trace.isNullOrEmpty()) {
                fields.add(field("Trace", "```\n$trace\n```"))
            }
            return mapOf(
                "title" to "[$exceptionClass]",
                "color" to 0xFF0000,
                "fields" to fields,
            )
        }

        private fun field(name: String, value: String) =
            mapOf("name" to name, "value" to value, "inline" to false)
    }
}
