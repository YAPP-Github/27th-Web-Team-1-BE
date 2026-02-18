package kr.co.lokit.api.domain.map.application.port.`in`

import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.PlaceSearchReadModel

interface SearchLocationUseCase {
    fun getLocationInfo(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel

    fun searchPlaces(query: String): PlaceSearchReadModel
}
