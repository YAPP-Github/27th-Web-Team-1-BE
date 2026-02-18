package kr.co.lokit.api.common.ratelimit

import java.net.URI

object RateLimitBypassPolicy {
    fun shouldBypass(
        serverName: String?,
        requestUri: String?,
        referer: String?,
    ): Boolean {
        val normalizedServer = serverName?.trim()?.lowercase().orEmpty()
        if (normalizedServer.contains("local")) {
            return true
        }
        val refererUri = parseUri(referer) ?: return false
        val refererHost = refererUri.host?.lowercase().orEmpty()
        if (refererHost != normalizedServer || refererHost.isBlank()) {
            return false
        }
        return refererUri.path.isSwaggerPath()
    }

    private fun parseUri(value: String?): URI? =
        value
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { URI.create(it) }.getOrNull()
            }

    private fun String?.isSwaggerPath(): Boolean =
        this?.let { path ->
            path.contains("/swagger") || path.contains("/v3/api-docs")
        }.orFalse()

    private fun Boolean?.orFalse(): Boolean = this ?: false
}
