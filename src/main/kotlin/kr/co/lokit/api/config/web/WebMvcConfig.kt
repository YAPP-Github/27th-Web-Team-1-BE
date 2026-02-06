package kr.co.lokit.api.config.web

import kr.co.lokit.api.config.security.CurrentUserArgumentResolver
import kr.co.lokit.api.config.security.CurrentUserIdArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.ShallowEtagHeaderFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver,
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
    private val cacheControlInterceptor: CacheControlInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdArgumentResolver)
        resolvers.add(currentUserArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(cacheControlInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/actuator/**",
                "/auth/**",
                "/swagger/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/docs/**",
            )
    }

    @Bean
    fun shallowEtagHeaderFilter() = ShallowEtagHeaderFilter()

    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer.apply {
            useRequestHeader("X-API-VERSION")
            setDefaultVersion("1.0")
        }
    }
}
