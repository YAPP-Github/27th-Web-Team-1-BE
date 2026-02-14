package kr.co.lokit.api.config.cache

object CacheNames {
    const val REVERSE_GEOCODE = "reverseGeocode"
    const val SEARCH_PLACES = "searchPlaces"
    const val USER_DETAILS = "userDetails"
    const val PHOTO = "photo"
    const val ALBUM = "album"
    const val ALBUM_COUPLE = "albumCouple"
    const val USER_COUPLE = "userCouple"
    const val COUPLE_ALBUMS = "coupleAlbums"
    const val MAP_PHOTOS = "mapPhotos"
    const val MAP_CELLS = "mapCells"
    const val PRESIGNED_URL = "presignedUrl"
}

enum class CacheRegion(
    val cacheName: String,
) {
    REVERSE_GEOCODE(CacheNames.REVERSE_GEOCODE),
    SEARCH_PLACES(CacheNames.SEARCH_PLACES),
    USER_DETAILS(CacheNames.USER_DETAILS),
    PHOTO(CacheNames.PHOTO),
    ALBUM(CacheNames.ALBUM),
    ALBUM_COUPLE(CacheNames.ALBUM_COUPLE),
    USER_COUPLE(CacheNames.USER_COUPLE),
    COUPLE_ALBUMS(CacheNames.COUPLE_ALBUMS),
    MAP_PHOTOS(CacheNames.MAP_PHOTOS),
    MAP_CELLS(CacheNames.MAP_CELLS),
    PRESIGNED_URL(CacheNames.PRESIGNED_URL),
}

object CacheRegionGroups {
    val PERMISSION =
        arrayOf(
            CacheRegion.ALBUM,
            CacheRegion.PHOTO,
            CacheRegion.ALBUM_COUPLE,
        )
}
