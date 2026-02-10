package kr.co.lokit.api.config.security

import kr.co.lokit.api.config.web.CorsProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: AuthenticationFilter,
    private val corsProperties: CorsProperties,
    private val loginAuthenticationEntryPoint: LoginAuthenticationEntryPoint,
    private val loginAccessDeniedHandler: LoginAccessDeniedHandler,
) {
    private val logger: Logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/auth/register",
                        "/auth/login",
                        "/auth/kakao",
                        "/auth/kakao/callback",
                    ).permitAll()
                    .requestMatchers("/actuator/health", "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/docs/**",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(loginAuthenticationEntryPoint)
                    .accessDeniedHandler(loginAccessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("*")
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
                exposedHeaders = listOf("*")
                maxAge = 3600L
            }
        logger.info("allowedOrigins: {}", configuration.allowedOriginPatterns?.joinToString { "[$it], " })
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
