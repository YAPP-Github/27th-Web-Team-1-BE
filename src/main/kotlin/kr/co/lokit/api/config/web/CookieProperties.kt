package kr.co.lokit.api.config.web

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "cookie")
class CookieProperties(
    var secure: Boolean = false,
)
