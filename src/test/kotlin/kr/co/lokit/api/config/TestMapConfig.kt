package kr.co.lokit.api.config

import kr.co.lokit.api.domain.map.application.port.ClusterPhotoProjection
import kr.co.lokit.api.domain.map.application.port.ClusterProjection
import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.application.port.PhotoProjection
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestMapConfig {

    @Bean
    @Primary
    fun mapQueryPort(): MapQueryPort = object : MapQueryPort {
        override fun findClustersWithinBBox(
            west: Double,
            south: Double,
            east: Double,
            north: Double,
            gridSize: Double,
            userId: Long?,
            albumId: Long?,
        ): List<ClusterProjection> = emptyList()

        override fun findPhotosWithinBBox(
            west: Double,
            south: Double,
            east: Double,
            north: Double,
            userId: Long?,
            albumId: Long?,
        ): List<PhotoProjection> = emptyList()

        override fun findPhotosInGridCell(
            west: Double,
            south: Double,
            east: Double,
            north: Double,
            userId: Long?,
        ): List<ClusterPhotoProjection> = emptyList()
    }
}
