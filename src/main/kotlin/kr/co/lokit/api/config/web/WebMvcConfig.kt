package kr.co.lokit.api.config.web

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer.apply {
            useRequestHeader("X-API-VERSION")
            setDefaultVersion("1.0")
        }
    }
}
