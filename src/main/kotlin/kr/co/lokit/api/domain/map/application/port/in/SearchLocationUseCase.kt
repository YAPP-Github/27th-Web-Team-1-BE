package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceSearchResponse

interface SearchLocationUseCase {
    fun getLocationInfo(
        longitude: Double,
        latitude: Double,
    ): LocationInfoResponse

    fun searchPlaces(query: String): PlaceSearchResponse
}
