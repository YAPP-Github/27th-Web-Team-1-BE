package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.Places

interface MapClient {
    fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel

    fun searchPlaces(query: String): Places
}
