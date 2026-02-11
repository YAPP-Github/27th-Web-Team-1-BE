package kr.co.lokit.api.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                CaffeineCache(
                    "reverseGeocode",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    "searchPlaces",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .build(),
                ),
                CaffeineCache(
                    "userDetails",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    "photo",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(300)
                        .build(),
                ),
                CaffeineCache(
                    "album",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    "albumCouple",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    "userCouple",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    "coupleAlbums",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    "mapPhotos",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(400)
                        .build(),
                ),
                CaffeineCache(
                    "mapCells",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(400)
                        .build(),
                ),
                CaffeineCache(
                    "presignedUrl",
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
            ),
        )
        return cacheManager
    }
}
