package kr.co.lokit.api.domain.map.application.port

import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.PlaceReadModel
import kr.co.lokit.api.domain.map.domain.Places

interface MapClientPort {
    fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel

    fun searchPlaces(query: String): Places
}
