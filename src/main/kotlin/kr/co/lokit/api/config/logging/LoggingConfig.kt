package kr.co.lokit.api.config.logging

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class LoggingConfig : WebMvcConfigurer {

    @Bean
    fun mdcContextFilter(): MdcContextFilter = MdcContextFilter()

    @Bean
    fun mdcContextFilterRegistration(filter: MdcContextFilter): FilterRegistrationBean<MdcContextFilter> =
        FilterRegistrationBean(filter).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }

    @Bean
    fun loggingInterceptor(): LoggingInterceptor = LoggingInterceptor()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor())
            .addPathPatterns("/**")
    }
}
