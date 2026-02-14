package kr.co.lokit.api.config.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

class LoggingInterceptor(
    private val verbose: Boolean = false,
) : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_BODY_LENGTH = 1000
        private val LOGGABLE_CONTENT_TYPES =
            setOf(
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            )
        private val EXCLUDED_PREFIXES =
            setOf(
                "/actuator/",
                "/swagger",
                "/swagger-ui/",
                "/v3/api-docs",
                "/docs/",
                "/admin/",
            )
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (shouldSkipLogging(request)) {
            return true
        }

        setUserIdToMdc()
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (shouldSkipLogging(request)) {
            return
        }

        val startTime = request.getAttribute(MdcContextFilter.START_TIME_ATTR) as? Long
        val latency = if (startTime != null) System.currentTimeMillis() - startTime else 0

        val wrappedRequest =
            request.getAttribute(MdcContextFilter.WRAPPED_REQUEST_ATTR) as? ContentCachingRequestWrapper
        val wrappedResponse =
            request.getAttribute(MdcContextFilter.WRAPPED_RESPONSE_ATTR) as? ContentCachingResponseWrapper

        val status = response.status

        MDC.put("status", status.toString())
        MDC.put("latencyMs", latency.toString())

        if (verbose) {
            logVerbose(request, wrappedRequest, wrappedResponse, status, latency, ex)
        } else {
            logCompact(status, ex)
        }
    }

    private fun logVerbose(
        request: HttpServletRequest,
        wrappedRequest: ContentCachingRequestWrapper?,
        wrappedResponse: ContentCachingResponseWrapper?,
        status: Int,
        latency: Long,
        ex: Exception?,
    ) {
        val method = request.method
        val uri = request.requestURI
        val query = request.queryString?.let { "?$it" }.orEmpty()
        val requestBody = wrappedRequest?.let { getBody(it.contentAsByteArray, it.contentType) }.orEmpty()
        val responseBody = wrappedResponse?.let { getBody(it.contentAsByteArray, it.contentType) }.orEmpty()

        val sb = StringBuilder()
        sb.appendLine("$method $uri$query → $status (${latency}ms)")

        if (requestBody.isNotEmpty()) {
            sb.appendLine(">>> $requestBody")
        }

        val traces = RequestTrace.drain()
        traces.forEach { sb.appendLine("├ ${it.method} → ${it.durationMs}ms") }

        if (responseBody.isNotEmpty()) {
            sb.appendLine("\n  <<< $responseBody")
        }

        logAtLevel(status, ex, sb.toString())
    }

    private fun logCompact(
        status: Int,
        ex: Exception?,
    ) {
        logAtLevel(status, ex, "request completed")
    }

    private fun shouldSkipLogging(request: HttpServletRequest): Boolean =
        EXCLUDED_PREFIXES.any { request.requestURI.startsWith(it) }

    private fun setUserIdToMdc() {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            MDC.put(MdcContextFilter.USER_ID, authentication.name)
        }
    }

    private fun logAtLevel(
        status: Int,
        ex: Exception?,
        message: String,
    ) {
        when {
            ex != null -> log.error(message, ex)
            status >= 500 -> log.error(message)
            status >= 400 -> log.warn(message)
            else -> log.info(message)
        }
    }

    private fun getBody(
        content: ByteArray,
        contentType: String?,
    ): String {
        if (!isLoggableContentType(contentType) || content.isEmpty()) {
            return ""
        }
        val body = String(content, Charsets.UTF_8)
        return if (body.length > MAX_BODY_LENGTH) {
            body.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
        } else {
            body
        }
    }

    private fun isLoggableContentType(contentType: String?): Boolean {
        if (contentType == null) {
            return false
        }
        return LOGGABLE_CONTENT_TYPES.any { contentType.contains(it, ignoreCase = true) }
    }
}
