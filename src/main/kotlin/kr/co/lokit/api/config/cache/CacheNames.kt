package kr.co.lokit.api.config.cache

object CacheNames {
    const val REVERSE_GEOCODE = "reverseGeocode"
    const val SEARCH_PLACES = "searchPlaces"

    //    const val USER_DETAILS = "userDetails"
    const val PHOTO = "photo"
    const val ALBUM = "album"

    //    const val ALBUM_COUPLE = "albumCouple"
//    const val USER_COUPLE = "userCouple"
//    const val COUPLE_ALBUMS = "coupleAlbums"
    const val MAP_PHOTOS = "mapPhotos"
    const val MAP_CELLS = "mapCells"
    const val PRESIGNED_URL = "presignedUrl"
}

enum class CacheRegion(
    val cacheName: String,
) {
    //    USER_DETAILS(CacheNames.USER_DETAILS),
    PHOTO(CacheNames.PHOTO),
    ALBUM(CacheNames.ALBUM),
//    ALBUM_COUPLE(CacheNames.ALBUM_COUPLE),
//    USER_COUPLE(CacheNames.USER_COUPLE),
//    COUPLE_ALBUMS(CacheNames.COUPLE_ALBUMS),
}

object CacheRegionGroups {
    val PERMISSION =
        arrayOf(
            CacheRegion.ALBUM,
            CacheRegion.PHOTO,
//            CacheRegion.ALBUM_COUPLE,
        )
}
