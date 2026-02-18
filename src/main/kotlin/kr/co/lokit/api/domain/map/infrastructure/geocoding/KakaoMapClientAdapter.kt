package kr.co.lokit.api.domain.map.infrastructure.geocoding

import kr.co.lokit.api.domain.map.application.port.MapClientPort
import kr.co.lokit.api.domain.map.domain.LocationInfoReadModel
import kr.co.lokit.api.domain.map.domain.Places
import org.springframework.stereotype.Component

@Component
class KakaoMapClientAdapter(
    private val kakaoMapClient: KakaoMapClient,
) : MapClientPort {
    override fun reverseGeocode(
        longitude: Double,
        latitude: Double,
    ): LocationInfoReadModel = kakaoMapClient.reverseGeocode(longitude, latitude)

    override fun searchPlaces(query: String): Places = kakaoMapClient.searchPlaces(query)
}
