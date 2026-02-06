package kr.co.lokit.api.config.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class CacheControlInterceptor : HandlerInterceptor {
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?,
    ) {
        if (response.containsHeader(HttpHeaders.CACHE_CONTROL)) {
            return
        }

        val method = request.method
        val path = request.servletPath

        val cacheControl = resolveCacheControl(method, path)
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl)
    }

    private fun resolveCacheControl(
        method: String,
        path: String,
    ): String {
        if (method != "GET") {
            return NO_STORE
        }

        return CACHE_RULES.firstOrNull { it.matches(path) }?.cacheControl
            ?: DEFAULT_CACHE
    }

    private data class CacheRule(
        val prefix: String,
        val cacheControl: String,
        val pattern: Regex? = null,
    ) {
        fun matches(path: String): Boolean =
            if (pattern != null) {
                pattern.matches(path)
            } else {
                path.startsWith(prefix)
            }
    }

    companion object {
        private const val NO_STORE = "no-store"
        private const val DEFAULT_CACHE = "private, no-cache"

        private val CACHE_RULES =
            listOf(
                // no-store (auth, admin)
                CacheRule("/auth", NO_STORE),
                CacheRule("/admin", NO_STORE),
                // public-like
                CacheRule("/map/location", "private, max-age=3600"),
                CacheRule("/map/places/search", "private, max-age=3600"),
                // map data
                CacheRule("/map/albums/", "private, max-age=300"),
                CacheRule("/map/clusters/", "private, max-age=60"),
                CacheRule("/map/me", "private, max-age=30"),
                // photo detail (single photo by ID)
                CacheRule("/photos/", "private, max-age=300", Regex("/photos/\\d+")),
                // album photos list
                CacheRule("/photos/album/", "private, max-age=60"),
                // selectable albums
                CacheRule("/albums/selectable", "private, max-age=60"),
            )
    }
}
