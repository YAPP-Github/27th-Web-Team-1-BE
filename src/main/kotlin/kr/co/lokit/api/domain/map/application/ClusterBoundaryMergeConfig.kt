package kr.co.lokit.api.domain.map.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class ClusterBoundaryMergeConfig {
    @Bean
    @Primary
    fun clusterBoundaryMergeStrategy(
        legacyClusterBoundaryMergeStrategy: LegacyClusterBoundaryMergeStrategy,
        distanceBasedClusterBoundaryMergeStrategy: DistanceBasedClusterBoundaryMergeStrategy,
        @Value("\${map.cluster.boundary-merge.strategy:distance}") strategy: String,
    ): ClusterBoundaryMergeStrategy =
        when (strategy.lowercase()) {
            "legacy" -> legacyClusterBoundaryMergeStrategy
            "distance" -> distanceBasedClusterBoundaryMergeStrategy
            else -> distanceBasedClusterBoundaryMergeStrategy
        }
}
