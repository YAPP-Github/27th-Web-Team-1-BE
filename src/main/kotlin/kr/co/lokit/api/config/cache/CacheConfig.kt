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
                    CacheNames.REVERSE_GEOCODE,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.SEARCH_PLACES,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.USER_DETAILS,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.PHOTO,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(300)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.ALBUM,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.ALBUM_COUPLE,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.USER_COUPLE,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.COUPLE_ALBUMS,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(3, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.MAP_PHOTOS,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(400)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.MAP_CELLS,
                    Caffeine
                        .newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(400)
                        .build(),
                ),
                CaffeineCache(
                    CacheNames.PRESIGNED_URL,
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
