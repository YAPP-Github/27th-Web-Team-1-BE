package kr.co.lokit.api.domain.map.application

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class ClusterBoundaryMergeConfig {
    private val log = LoggerFactory.getLogger(ClusterBoundaryMergeConfig::class.java)

    @Bean
    @Primary
    fun clusterBoundaryMergeStrategy(
        @Value("\${map.cluster.boundary-merge.strategy:distance}") strategy: String,
    ): ClusterBoundaryMergeStrategy {
        val selected =
            when (ClusterBoundaryMergeType.from(strategy)) {
                ClusterBoundaryMergeType.LEGACY -> LegacyClusterBoundaryMergeStrategy()
                ClusterBoundaryMergeType.DISTANCE -> DistanceBasedClusterBoundaryMergeStrategy()
            }
        log.info("Cluster boundary merge strategy selected: {}", selected::class.simpleName)
        return selected
    }
}
