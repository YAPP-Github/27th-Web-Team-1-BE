package kr.co.lokit.api.domain.map.application.port

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse

interface MapClientPort {
    fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse

    fun searchPlaces(query: String): List<PlaceResponse>
}
