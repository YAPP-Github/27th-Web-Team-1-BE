package kr.co.lokit.api.config.feature

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableRetry
@EnableScheduling
@EnableJpaAuditing
@EnableConfigurationProperties
class FeatureConfig
