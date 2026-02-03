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

class LoggingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_BODY_LENGTH = 1000
        private val LOGGABLE_CONTENT_TYPES = setOf(
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        )
        private val EXCLUDED_PATHS = setOf("/api/actuator/health")
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

        val wrappedRequest = request.getAttribute(MdcContextFilter.WRAPPED_REQUEST_ATTR) as? ContentCachingRequestWrapper
        val wrappedResponse = request.getAttribute(MdcContextFilter.WRAPPED_RESPONSE_ATTR) as? ContentCachingResponseWrapper

        val status = response.status
        val requestBody = wrappedRequest?.let { getRequestBody(it) } ?: ""
        val responseBody = wrappedResponse?.let { getResponseBody(it) } ?: ""

        MDC.put("status", status.toString())
        MDC.put("latencyMs", latency.toString())

        if (requestBody.isNotEmpty()) {
            MDC.put("requestBody", requestBody)
        }
        if (responseBody.isNotEmpty()) {
            MDC.put("responseBody", responseBody)
        }

        logRequest(status, ex)
    }

    private fun shouldSkipLogging(request: HttpServletRequest): Boolean =
        EXCLUDED_PATHS.contains(request.requestURI)

    private fun setUserIdToMdc() {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.name != "anonymousUser") {
            MDC.put(MdcContextFilter.USER_ID, authentication.name)
        }
    }

    private fun logRequest(status: Int, ex: Exception?) {
        when {
            ex != null -> log.error("request failed with exception", ex)
            status >= 500 -> log.error("request completed")
            status >= 400 -> log.warn("request completed")
            else -> log.info("request completed")
        }
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        if (!isLoggableContentType(request.contentType)) {
            return ""
        }

        val content = request.contentAsByteArray
        if (content.isEmpty()) {
            return ""
        }

        val body = String(content, Charsets.UTF_8)
        return truncateBody(body)
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        if (!isLoggableContentType(response.contentType)) {
            return ""
        }

        val content = response.contentAsByteArray
        if (content.isEmpty()) {
            return ""
        }

        val body = String(content, Charsets.UTF_8)
        return truncateBody(body)
    }

    private fun truncateBody(body: String): String =
        if (body.length > MAX_BODY_LENGTH) {
            body.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
        } else {
            body
        }

    private fun isLoggableContentType(contentType: String?): Boolean {
        if (contentType == null) return false
        return LOGGABLE_CONTENT_TYPES.any { contentType.contains(it, ignoreCase = true) }
    }
}
