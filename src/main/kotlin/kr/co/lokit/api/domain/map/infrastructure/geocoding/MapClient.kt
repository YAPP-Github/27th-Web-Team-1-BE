package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse

interface MapClient {
    fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse

    fun searchPlaces(query: String): List<PlaceResponse>
}
