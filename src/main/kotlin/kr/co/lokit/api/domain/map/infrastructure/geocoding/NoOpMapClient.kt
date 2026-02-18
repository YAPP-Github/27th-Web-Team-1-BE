package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.Places
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

@Component
@ConditionalOnMissingBean(KakaoMapClient::class)
class NoOpMapClient : MapClient {
    override fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel =
        LocationInfoReadModel(
            address = null,
            placeName = null,
            regionName = null,
        )

    override fun searchPlaces(query: String): Places = Places.empty()
}
