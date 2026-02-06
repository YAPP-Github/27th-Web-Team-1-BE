package kr.co.lokit.api.domain.map.application

import kr.co.lokit.api.domain.map.application.port.MapQueryPort
import kr.co.lokit.api.domain.map.domain.BBox
import kr.co.lokit.api.domain.map.domain.GridValues
import kr.co.lokit.api.domain.map.dto.MapPhotosResponse
import kr.co.lokit.api.domain.map.mapping.toMapPhotoResponse
import kr.co.lokit.api.domain.map.mapping.toResponse
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.floor

@Service
class MapPhotosCacheService(
    private val mapQueryPort: MapQueryPort,
    private val cacheManager: CacheManager,
) {

    fun evictForCouple(coupleId: Long) {
        val cache = cacheManager.getCache("mapPhotos") ?: return
        val nativeCache = (cache as CaffeineCache).nativeCache
        nativeCache.asMap().keys.removeIf { key ->
            (key as String).contains("_c$coupleId")
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key = "#cacheKey",
        unless = "(#result.clusters == null || #result.clusters.isEmpty()) && (#result.photos == null || #result.photos.isEmpty())",
    )
    fun getClusteredPhotos(
        zoom: Int,
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        cacheKey: String,
    ): MapPhotosResponse {
        val gridSize = GridValues.getGridSize(zoom - 1)

        val clusters =
            mapQueryPort.findClustersWithinBBox(
                west = bbox.west,
                south = bbox.south,
                east = bbox.east,
                north = bbox.north,
                gridSize = gridSize,
                coupleId = coupleId,
                albumId = albumId,
            )

        return MapPhotosResponse(
            clusters = clusters.map { it.toResponse(zoom) },
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["mapPhotos"],
        key = "#cacheKey",
        unless = "(#result.clusters == null || #result.clusters.isEmpty()) && (#result.photos == null || #result.photos.isEmpty())",
    )
    fun getIndividualPhotos(
        bbox: BBox,
        coupleId: Long?,
        albumId: Long?,
        cacheKey: String,
    ): MapPhotosResponse {
        val photos = mapQueryPort.findPhotosWithinBBox(
            west = bbox.west,
            south = bbox.south,
            east = bbox.east,
            north = bbox.north,
            coupleId = coupleId,
            albumId = albumId,
        )

        return MapPhotosResponse(
            photos = photos.map { it.toMapPhotoResponse() },
        )
    }

    fun buildCacheKey(zoom: Int, bbox: BBox, coupleId: Long?, albumId: Long?): String {
        val precision = if (zoom < GridValues.CLUSTER_ZOOM_THRESHOLD) 100 else 10000
        val w = floor(bbox.west * precision) / precision
        val s = floor(bbox.south * precision) / precision
        val e = floor(bbox.east * precision) / precision
        val n = floor(bbox.north * precision) / precision
        return buildString {
            append("z$zoom")
            append("_${w}_${s}_${e}_${n}")
            if (coupleId != null) append("_c$coupleId")
            if (albumId != null) append("_a$albumId")
        }
    }
}
