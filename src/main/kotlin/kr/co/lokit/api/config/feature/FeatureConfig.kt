package kr.co.lokit.api.config.feature

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableJpaAuditing
class FeatureConfig
