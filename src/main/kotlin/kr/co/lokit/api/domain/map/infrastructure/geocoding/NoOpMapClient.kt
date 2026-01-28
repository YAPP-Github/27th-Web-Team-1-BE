package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import kr.co.lokit.api.domain.map.dto.PlaceResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(KakaoMapClient::class)
class NoOpMapClient : MapClient {
    override fun reverseGeocode(longitude: Double, latitude: Double): LocationInfoResponse =
        LocationInfoResponse(
            address = null,
            placeName = null,
            regionName = null,
        )

    override fun searchPlaces(query: String): List<PlaceResponse> = emptyList()
}
