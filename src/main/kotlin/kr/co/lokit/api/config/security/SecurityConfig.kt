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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
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
            .headers { headers ->
                headers
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives(buildCspPolicy())
                    }.httpStrictTransportSecurity { hsts ->
                        hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000)
                    }.referrerPolicy { referrer ->
                        referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN,
                        )
                    }.frameOptions { it.deny() }
                    .xssProtection { it.disable() }
            }.authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers(
                        "/auth/kakao",
                        "/auth/kakao/callback",
                        "/emails",
                        "/emails/**",
                    ).permitAll()
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

    private fun buildCspPolicy(): String {
        val allowedOrigins =
            corsProperties.allowedOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        val connectSrc =
            buildString {
                append("'self'")
                allowedOrigins.forEach { origin ->
                    append(" ").append(origin)
                }
            }

        return """
            default-src 'self';
            script-src 'self';
            style-src 'self';
            img-src 'self' data: https:;
            font-src 'self' data:;
            connect-src $connectSrc;
            frame-ancestors 'none';
            """.trimIndent().replace("\n", " ")
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins =
            corsProperties.allowedOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        require(origins.isNotEmpty()) {
            "CORS allowedOrigins must not be empty"
        }

        val configuration =
            CorsConfiguration().apply {
                allowedOrigins = origins
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                allowedHeaders = listOf("*")
                exposedHeaders = listOf("Authorization")
                allowCredentials = true
                maxAge = 3600
            }

        logger.info("CORS configured origins={}", origins.joinToString(","))
        logger.info("CSP connect-src={}", buildCspPolicy())

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
