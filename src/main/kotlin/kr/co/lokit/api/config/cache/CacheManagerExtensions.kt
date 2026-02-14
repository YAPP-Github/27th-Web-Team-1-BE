package kr.co.lokit.api.config.cache

import org.springframework.cache.CacheManager

fun CacheManager.evictKey(
    cacheName: String,
    key: Any,
) {
    getCache(cacheName)?.evict(key)
}

fun CacheManager.evictKey(
    cacheRegion: CacheRegion,
    key: Any,
) {
    getCache(cacheRegion.cacheName)?.evict(key)
}

fun CacheManager.clearCaches(vararg cacheNames: String) {
    cacheNames.forEach { cacheName ->
        getCache(cacheName)?.clear()
    }
}

fun CacheManager.clearCaches(vararg cacheRegions: CacheRegion) {
    cacheRegions.forEach { cacheRegion ->
        getCache(cacheRegion.cacheName)?.clear()
    }
}

fun CacheManager.clearAllCaches() {
    cacheNames.forEach { cacheName ->
        getCache(cacheName)?.clear()
    }
}

fun CacheManager.clearPermissionCaches() {
    clearCaches(*CacheRegionGroups.PERMISSION)
}

fun CacheManager.evictUserCoupleCache(vararg userIds: Long) {
    userIds.forEach { userId ->
        evictKey(CacheRegion.USER_COUPLE, userId)
    }
}
