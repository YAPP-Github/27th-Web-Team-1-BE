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
                    Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(10_000)
                        .build(),
                ),
                CaffeineCache(
                    "searchPlaces",
                    Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(1_000)
                        .build(),
                ),
                CaffeineCache(
                    "userDetails",
                    Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .build(),
                ),
                CaffeineCache(
                    "photo",
                    Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .build(),
                ),
                CaffeineCache(
                    "album",
                    Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .build(),
                ),
                CaffeineCache(
                    "albumCouple",
                    Caffeine.newBuilder()
                        .expireAfterWrite(3, TimeUnit.HOURS)
                        .maximumSize(10_000)
                        .build(),
                ),
                CaffeineCache(
                    "coupleMembership",
                    Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(50_000)
                        .build(),
                ),
                CaffeineCache(
                    "userAlbums",
                    Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build(),
                ),
                CaffeineCache(
                    "mapPhotos",
                    Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .maximumSize(50_000)
                        .build(),
                ),
            ),
        )
        return cacheManager
    }
}
