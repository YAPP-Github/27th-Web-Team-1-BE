package kr.co.lokit.api.config.logging

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class LoggingConfig {
    @Bean
    fun mdcLoggingFilter(): MdcLoggingFilter = MdcLoggingFilter()

    @Bean
    fun mdcLoggingFilterRegistration(filter: MdcLoggingFilter): FilterRegistrationBean<MdcLoggingFilter> =
        FilterRegistrationBean(filter).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
