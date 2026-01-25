package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.dto.LocationInfoResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(KakaoGeocodingClient::class)
class NoOpGeocodingClient : GeocodingClient {
    override fun reverseGeocode(longitude: Double, latitude: Double): LocationInfoResponse =
        LocationInfoResponse(
            address = null,
            placeName = null,
            regionName = null,
        )
}
