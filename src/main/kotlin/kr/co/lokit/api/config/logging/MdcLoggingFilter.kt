package kr.co.lokit.api.config.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.UUID

class MdcLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val REQUEST_ID = "requestId"
        const val REQUEST_URI = "requestUri"
        const val REQUEST_METHOD = "requestMethod"
        const val CLIENT_IP = "clientIp"
        const val STATUS = "status"
        const val LATENCY = "latencyMs"
        const val REQUEST_BODY = "requestBody"
        const val RESPONSE_BODY = "responseBody"

        private const val MAX_BODY_LENGTH = 1000
        private val LOGGABLE_CONTENT_TYPES =
            setOf(
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_PLAIN_VALUE,
                MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_BODY_LENGTH)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        val start = System.currentTimeMillis()

        try {
            MDC.put(REQUEST_ID, generateRequestId())
            MDC.put(REQUEST_URI, request.requestURI)
            MDC.put(REQUEST_METHOD, request.method)
            MDC.put(CLIENT_IP, getClientIp(request))

            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val latency = System.currentTimeMillis() - start
            val status = wrappedResponse.status

            MDC.put(STATUS, status.toString())
            MDC.put(LATENCY, latency.toString())

            val requestBody = getRequestBody(wrappedRequest)
            val responseBody = getResponseBody(wrappedResponse)

            if (requestBody.isNotEmpty()) {
                MDC.put(REQUEST_BODY, requestBody)
            }
            if (responseBody.isNotEmpty()) {
                MDC.put(RESPONSE_BODY, responseBody)
            }

            logRequest(status, latency, requestBody, responseBody)

            wrappedResponse.copyBodyToResponse()
            MDC.clear()
        }
    }

    private fun logRequest(
        status: Int,
        latency: Long,
        requestBody: String,
        responseBody: String,
    ) {
        val sb = StringBuilder()
        sb.append("status=$status, latency=${latency}ms")

        if (requestBody.isNotEmpty()) {
            sb.append(", request=$requestBody")
        }

        if (isErrorStatus(status) && responseBody.isNotEmpty()) {
            sb.append(", response=$responseBody")
        }

        if (isErrorStatus(status)) {
            log.warn("completed: {}", sb.toString())
        } else {
            log.info("completed: {}", sb.toString())
        }
    }

    private fun isErrorStatus(status: Int): Boolean = status >= 400

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        if (!isLoggableContentType(request.contentType)) {
            return ""
        }

        val content = request.contentAsByteArray
        if (content.isEmpty()) {
            return ""
        }

        val body = String(content, Charsets.UTF_8)
        val truncated =
            if (body.length > MAX_BODY_LENGTH) {
                body.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
            } else {
                body
            }

        return truncated
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
        val truncated =
            if (body.length > MAX_BODY_LENGTH) {
                body.substring(0, MAX_BODY_LENGTH) + "...(truncated)"
            } else {
                body
            }

        return truncated
    }

    private fun isLoggableContentType(contentType: String?): Boolean {
        if (contentType == null) return false
        return LOGGABLE_CONTENT_TYPES.any { contentType.contains(it, ignoreCase = true) }
    }

    private fun generateRequestId(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 8)

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",").first().trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr ?: "unknown"
    }
}
