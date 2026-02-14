package kr.co.lokit.api.config.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.*

class MdcContextFilter : OncePerRequestFilter() {
    companion object {
        const val REQUEST_ID = "requestId"
        const val REQUEST_URI = "requestUri"
        const val REQUEST_METHOD = "requestMethod"
        const val CLIENT_IP = "clientIp"
        const val QUERY_STRING = "queryString"
        const val USER_ID = "userId"
        const val START_TIME_ATTR = "requestStartTime"
        const val WRAPPED_REQUEST_ATTR = "wrappedRequest"
        const val WRAPPED_RESPONSE_ATTR = "wrappedResponse"

        private const val MAX_BODY_LENGTH = 1000
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_BODY_LENGTH)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        try {
            RequestTrace.init()
            MDC.put(REQUEST_ID, generateRequestId())
            MDC.put(REQUEST_URI, request.requestURI)
            MDC.put(REQUEST_METHOD, request.method)
            MDC.put(CLIENT_IP, getClientIp(request))
            request.queryString?.let { MDC.put(QUERY_STRING, it) }

            request.setAttribute(START_TIME_ATTR, System.currentTimeMillis())
            request.setAttribute(WRAPPED_REQUEST_ATTR, wrappedRequest)
            request.setAttribute(WRAPPED_RESPONSE_ATTR, wrappedResponse)

            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            wrappedResponse.copyBodyToResponse()
            MDC.clear()
        }
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
