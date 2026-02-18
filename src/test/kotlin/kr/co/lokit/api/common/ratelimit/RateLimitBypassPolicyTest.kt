package kr.co.lokit.api.common.ratelimit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RateLimitBypassPolicyTest {
    @Test
    fun `server name containing local bypasses rate limit`() {
        assertTrue(
            RateLimitBypassPolicy.shouldBypass(
                serverName = "api.local",
                requestUri = "/api/couples/invites",
                referer = null,
            ),
        )
    }

    @Test
    fun `swagger from same server bypasses rate limit`() {
        assertTrue(
            RateLimitBypassPolicy.shouldBypass(
                serverName = "dev.lokit.co.kr",
                requestUri = "/api/couples/invites",
                referer = "https://dev.lokit.co.kr/api/swagger/index.html",
            ),
        )
    }

    @Test
    fun `swagger from different server does not bypass rate limit`() {
        assertFalse(
            RateLimitBypassPolicy.shouldBypass(
                serverName = "dev.lokit.co.kr",
                requestUri = "/api/couples/invites",
                referer = "https://other.example.com/api/swagger/index.html",
            ),
        )
    }

    @Test
    fun `non swagger referer does not bypass rate limit`() {
        assertFalse(
            RateLimitBypassPolicy.shouldBypass(
                serverName = "dev.lokit.co.kr",
                requestUri = "/api/couples/invites",
                referer = "https://dev.lokit.co.kr/app",
            ),
        )
    }
}
